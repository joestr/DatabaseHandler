package net.dertod2.DatabaseHandler.Binary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Updater {
    /**
     * The Path to the maven repo folder containing the different version folders
     * and the<br />
     * maven-metadata.xml file with the last slash
     */
    public static final String mavenUri = "https://nexus.joestr.xyz/service/rest/repository/browse/maven-snapshots/net/dertod2/DatabaseHandler/";
    public static final File updateFolder = Bukkit.getUpdateFolderFile();
    public static final File tempFolder = DatabaseHandler.getInstance().getDataFolder();
    public static final boolean downloadUpdate = DatabaseHandler.getConfiguration().getBoolean("download-updates",
            false);
    public static final String permission = "databasehandler.updatemessage";
    public static final String informer = ChatColor.GOLD + "[DatabaseHandler] " + ChatColor.RESET;
    public static final boolean autoCheck = DatabaseHandler.getConfiguration().getBoolean("check-for-updates", true);
    public static final int checkSeconds = 43200; // 12 hours
    public static final int informSeconds = 300; // 5 minutes

    private static int checks = 0;

    public final File pluginFile;

    private boolean updateAvailable = false;
    private URL downloadUri;

    private String latestString;
    private long latestTimestamp;
    private String artifactId;

    private volatile boolean isFetching = false;
    private volatile boolean hasDownloaded = false;

    public Thread delayCheckThread;

    public Updater(File pluginFile) {
        this.pluginFile = pluginFile;

        this.delayCheckThread = new Thread(new Runnable() {
            public void run() {
                while (Thread.currentThread() == delayCheckThread) {
                    if (!updateAvailable() && checks++ >= checkSeconds) {
                        check(Bukkit.getConsoleSender());
                        checks = 0;
                    } else if (updateAvailable() && checks++ >= informSeconds) {
                        check(Bukkit.getConsoleSender());
                        checks = 0;
                    }

                    try {
                        Thread.sleep(1000L);
                    } catch (Exception exc) {
                    }
                }
            }
        });

        if (Updater.autoCheck)
            this.delayCheckThread.start();
    }

    /**
     * This method checks for updates
     */
    public void check(CommandSender sender) {
        if (this.isFetching)
            return;

        if (this.updateAvailable) {
            if (Updater.downloadUpdate) {
                if (this.hasDownloaded) {
                    this.sendMessage(
                            ChatColor.DARK_GREEN + "Download of version " + ChatColor.GOLD + this.latestString
                                    + ChatColor.DARK_GREEN + " finished. Restart the server to update to new version",
                            sender);
                } else {
                    this.sendMessage(ChatColor.DARK_GREEN + "Update found. Downloading new version...", sender);
                }
            } else {
                this.sendMessage(ChatColor.DARK_GREEN + "Found new version (" + ChatColor.GOLD + this.latestString
                        + ChatColor.DARK_GREEN + ") of plugin.", sender);
                this.sendMessage(
                        ChatColor.DARK_GREEN + "Download here: " + ChatColor.GOLD + this.downloadUri.toString(),
                        sender);
            }

            return;
        }

        this.isFetching = true;

        new Thread(new Runnable() {
            public void run() {
                fetchData();
            }
        }).start();
    }

    public boolean updateAvailable() {
        return this.updateAvailable;
    }

    public boolean downloadUpdate() {
        return Updater.downloadUpdate;
    }

    public boolean downloadedUpdate() {
        return this.hasDownloaded;
    }

    public String latestVersion() {
        return this.latestString;
    }

    public String downloadUri() {
        return this.downloadUri.toString();
    }

    private void fetchData() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            URL url;
            File tmpFile;

            url = new URL(Updater.mavenUri + "maven-metadata.xml");
            tmpFile = new File(Updater.tempFolder, "updTmpFile.xml");
            Updater.tempFolder.mkdirs();
            if (tmpFile.exists())
                tmpFile.delete();
            downloadFile(url, tmpFile);

            String mainlineVersion = getMainlineVersion(documentBuilder.parse(tmpFile));

            url = new URL(Updater.mavenUri + mainlineVersion + "/maven-metadata.xml");
            if (tmpFile.exists())
                tmpFile.delete();
            downloadFile(url, tmpFile);

            readUpdateInfo(documentBuilder.parse(tmpFile));

            // Now check the versions (add +0100 to latest)
            if ((this.latestTimestamp + 3600000) > this.pluginFile.lastModified()) {
                this.updateAvailable = true;
                this.downloadUri = new URL(Updater.mavenUri + mainlineVersion + "/" + this.artifactId + "-"
                        + this.extractMainlineVersion(mainlineVersion) + "-" + latestString + ".jar");

                this.isFetching = false;
                this.check(null); // Force re-call of method to output informations
                this.check(Bukkit.getConsoleSender());
                if (Updater.downloadUpdate)
                    this.downloadUpdateFile();
            } else {
                this.updateAvailable = false;
                this.downloadUri = null;

                this.sendMessage(ChatColor.DARK_GREEN + "Plugin is up to date.", Bukkit.getConsoleSender());
            }
        } catch (Exception exc) {
            this.sendMessage(ChatColor.RED + "Can't check for updates. Server can not be reached!",
                    Bukkit.getConsoleSender());
            exc.printStackTrace();
        }

        this.isFetching = false;
    }

    private void downloadUpdateFile() {
        if (!this.updateAvailable)
            return;

        try {
            URL url = this.downloadUri;
            URLConnection urlConnection = url.openConnection();

            Updater.updateFolder.mkdirs();

            InputStream inputStream = urlConnection.getInputStream();
            OutputStream outputStream = new FileOutputStream(new File(Updater.updateFolder, this.pluginFile.getName()));

            byte[] buffer = new byte[512];
            int read;

            while ((read = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            this.hasDownloaded = true;
            this.check(null); // Force re-call of method to output informations
            this.check(Bukkit.getConsoleSender());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private int sendMessage(String message, CommandSender sender) {
        int send = 0;

        if (sender != null) {
            if (sender.hasPermission(Updater.permission)) {
                sender.sendMessage(Updater.informer + message);
                send++;
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(Updater.permission)) {
                    player.sendMessage(Updater.informer + message);
                    send++;
                }
            }
        }

        return send;
    }

    private String getMainlineVersion(Document document) {
        Element metadata = (Element) document.getElementsByTagName("metadata").item(0);
        Element versioning = (Element) metadata.getElementsByTagName("versioning").item(0);
        Element versions = (Element) versioning.getElementsByTagName("versions").item(0);
        NodeList version = versions.getElementsByTagName("version");

        return version.item(version.getLength() - 1).getTextContent();
    }

    private String extractMainlineVersion(String mainlineVersion) {
        return mainlineVersion.replace("-RELEASE", "").replace("-SNAPSHOT", "");
    }

    private void downloadFile(URL url, File target) throws IOException {
        if (target.exists())
            target.delete();

        InputStream inputStream = url.openConnection().getInputStream();
        OutputStream outputStream = new FileOutputStream(target);

        byte[] buffer = new byte[512];
        int read = 0;

        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }

        outputStream.flush();
        outputStream.close();

        inputStream.close();
    }

    private void readUpdateInfo(Document document) throws DOMException, ParseException {
        Element metadata = (Element) document.getElementsByTagName("metadata").item(0);
        Element versioning = (Element) metadata.getElementsByTagName("versioning").item(0);

        Element snapshot = (Element) versioning.getElementsByTagName("snapshot").item(0);
        Element lastUpdated = (Element) versioning.getElementsByTagName("lastUpdated").item(0);

        this.latestString = snapshot.getElementsByTagName("timestamp").item(0).getTextContent() + "-"
                + snapshot.getElementsByTagName("buildNumber").item(0).getTextContent();
        this.latestTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").parse(lastUpdated.getTextContent()).getTime();
        this.artifactId = metadata.getElementsByTagName("artifactId").item(0).getTextContent();
    }
}