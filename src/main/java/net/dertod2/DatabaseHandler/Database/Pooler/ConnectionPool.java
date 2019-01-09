package net.dertod2.DatabaseHandler.Database.Pooler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.ImmutableList;

import net.dertod2.DatabaseHandler.Exceptions.NoPooledConnectionAvailableException;

public class ConnectionPool {
    private final PoolSettings poolSettings;
    private PoolStatistics poolStatistics;

    private volatile List<PooledConnection> availableList = new CopyOnWriteArrayList<PooledConnection>(); // new
                                                                                                          // ArrayList<PooledConnection>();
    private volatile List<PooledConnection> loanedList = new CopyOnWriteArrayList<PooledConnection>(); // new
                                                                                                       // ArrayList<PooledConnection>();

    private volatile long lastConnectionFetched;

    private volatile Thread watchThread;

    private boolean debugMode = false;

    private volatile boolean forceWatch = false;

    public ConnectionPool(PoolSettings poolSettings) {
        this.poolSettings = poolSettings;
        this.poolStatistics = new PoolStatistics();
    }

    public boolean testCredentials() {
        if (this.poolSettings.minPoolSize > this.poolSettings.maxPoolSize)
            return false;

        Connection connection = this.startConnection();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exc) {
            }
            return true;
        }

        return false;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean getDebugMode() {
        return this.debugMode;
    }

    public PoolStatistics getStatistics() {
        return this.poolStatistics;
    }

    public PoolSettings getSettings() {
        return this.poolSettings;
    }

    public void startPool() {
        if (!this.testCredentials())
            return;

        this.watchThread = new Thread(new Runnable() {
            public void run() {
                thread();
            }
        });
        this.watchThread.setPriority(Thread.MAX_PRIORITY);
        this.watchThread.setDaemon(true);

        this.watchThread.start();
        try {
            Thread.sleep(500L);
        } catch (Exception exc) {
        } // Let the main thread sleep for 500 ms
    }

    /**
     * Executes the Watcher Task
     **/
    private void thread() {
        Thread currentThread = Thread.currentThread();
        while (this.watchThread == currentThread) {
            long startNanos = System.currentTimeMillis();

            List<PooledConnection> removeableList = new ArrayList<PooledConnection>();

            try {
                // Work loaned Connections
                for (PooledConnection pooledConnection : this.loanedList) {
                    try {
                        if (pooledConnection.returnToPool) { // Returns connection to the Pool
                            pooledConnection.isInPool = true;

                            pooledConnection.autoClose = true;
                            pooledConnection.currentUser = "None";

                            removeableList.add(pooledConnection);
                            this.availableList.add(pooledConnection);

                            this.poolStatistics.returnedToPool++;

                            if (this.debugMode)
                                System.out.println(
                                        "Returned Pooled Connection to the Pool (fetcher executed close method)...");
                        } else if (pooledConnection.getLoanedtime() > this.poolSettings.maxLoanedTime
                                && pooledConnection.autoClose) {
                            try {
                                pooledConnection.rawConnection.close();
                            } catch (SQLException exc) {
                            }
                            removeableList.add(pooledConnection);
                            this.poolStatistics.maxLoanedTimeReached++;
                            if (this.debugMode)
                                System.out.println("Force closed Pooled Connection 'cause of maxLoanedtime reached...");
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }

                for (PooledConnection pooledConnection : removeableList) {
                    this.loanedList.remove(pooledConnection);
                }

                removeableList.clear();

                // Work available Connections
                for (PooledConnection pooledConnection : this.availableList) {
                    try {
                        if (pooledConnection.getLifetime() >= this.poolSettings.maxLifetime) {
                            removeableList.add(pooledConnection);
                            this.poolStatistics.maxLifeTimeReached++;
                            if (this.debugMode)
                                System.out.println("Closed Pooled Connection 'cause of maxLifetime reached...");
                        } else if (pooledConnection.getIdletime() >= this.poolSettings.maxIdletime) {
                            removeableList.add(pooledConnection);
                            this.poolStatistics.maxIdleTimeReached++;
                            if (this.debugMode)
                                System.out.println("Closed Pooled Connection 'cause of maxIdletime reached...");
                        } else if (!pooledConnection.rawConnection.isValid(1)) {
                            removeableList.add(pooledConnection);
                            this.poolStatistics.invalidConnection++;
                            if (this.debugMode)
                                System.out.println("Closed Pooled Connection 'cause of invalid raw connection...");
                        }
                    } catch (Exception exc) {
                    }
                }

                for (PooledConnection pooledConnection : removeableList) {
                    try {
                        pooledConnection.rawConnection.close();
                    } catch (SQLException exc) {
                    }
                    this.availableList.remove(pooledConnection); // Remove from pool
                }

                removeableList.clear();

                // Check Pool Size
                if (this.getLastFetchTime() < this.poolSettings.startSleepMode) {
                    while (this.availableList.size() < this.poolSettings.minPoolSize) {
                        if ((this.availableList.size() + this.loanedList.size()) >= this.poolSettings.maxPoolSize) {
                            this.poolStatistics.maxPoolSizeReached++;
                            break;
                        }

                        Connection connection = this.startConnection();
                        if (connection != null) {
                            this.poolStatistics.openedConnections++;
                            this.availableList.add(new PooledConnection(this, connection));
                            if (this.debugMode)
                                System.out
                                        .println("Opened new Pooled Connection cause not enough available Connections");
                        }
                    }
                }

                // Set Force Watch to false, in case it was setted to true
                this.forceWatch = false;

                // Wait before check again
                this.poolStatistics.lastWatcherDuration = System.currentTimeMillis() - startNanos;
                this.poolStatistics.watcherRuns++;

                try {
                    Thread.sleep(this.poolSettings.watcherTimer);
                } catch (Exception exc) {
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    private Connection startConnection() {
        try {
            return DriverManager.getConnection(this.poolSettings.jdbcUrl, this.poolSettings.username,
                    this.poolSettings.password);
        } catch (SQLException exc) {
            exc.printStackTrace();
            return null; // Close Pool ?
        }
    }

    /**
     * Returns an Connection out of the Pool<br />
     * Warning: When no connection is available but the Pool can open more
     * connections this will end in an Thread lock<br />
     * When no connections are available this will throw an Exception.
     * 
     * @return
     */
    public Connection getConnection() {
        this.lastConnectionFetched = System.currentTimeMillis();

        if (this.availableList.isEmpty()) {
            int openedConnections = this.availableList.size() + this.loanedList.size();

            if (openedConnections >= this.poolSettings.maxPoolSize) {
                this.poolStatistics.maxPoolSizeReachedWhileFetching++;
                throw new NoPooledConnectionAvailableException();
            } else {
                this.forceWatch = true;
                if (this.watchThread != null)
                    this.watchThread.interrupt();
                this.poolStatistics.threadLock++;
                while (this.forceWatch) {
                } // Thread Locking
            }
        }

        PooledConnection pooledConnection = this.availableList.remove(0);
        pooledConnection.loaned = System.currentTimeMillis();
        pooledConnection.returnToPool = false;
        pooledConnection.isInPool = false;

        pooledConnection.currentUser = this.getFetcher(Thread.currentThread().getStackTrace());

        this.loanedList.add(pooledConnection);

        if (this.debugMode)
            System.out.println("Fetched Pooled Connection out of pool...");

        return pooledConnection;
    }

    private String getFetcher(StackTraceElement[] stackTrace) {
        String fetcher = "Unknown";

        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();

            if (className.contains("net.dertod2") && !className.contains("DatabaseHandler")) {
                int beginIndex = className.indexOf(".", className.indexOf(".")) + 1; // plugin names -> ex.:
                                                                                     // net.dertod2.DatabaseHandler or
                                                                                     // me.author.pluginname
                int endIndex = className.indexOf(".", beginIndex);

                fetcher = className.substring(beginIndex, endIndex);
                break;
            }
        }

        return fetcher;
    }

    public int getAvailableConnections() {
        return this.availableList.size();
    }

    public int getLoanedConnections() {
        return this.loanedList.size();
    }

    public long getLastFetched() {
        return this.lastConnectionFetched;
    }

    public long getLastFetchTime() {
        return System.currentTimeMillis() - this.lastConnectionFetched;
    }

    /**
     * DO NOT USE THIS CONNECTIONS FOR WORK!<br />
     * Only to get statistics over the PooledConnection Objects
     * 
     * @return
     */
    public List<PooledConnection> getActiveConnections() {
        return ImmutableList.<PooledConnection>builder().addAll(this.availableList).addAll(this.loanedList).build();
    }

    public PoolStatistics restart() {
        if (this.watchThread != null) {
            this.watchThread.interrupt();
            this.watchThread = null;
        }

        for (PooledConnection pooledConnection : this.availableList) {
            try {
                pooledConnection.rawConnection.close();
            } catch (SQLException exc) {
            }
        }

        for (PooledConnection pooledConnection : this.loanedList) {
            pooledConnection.poolReference = null;
        }

        this.loanedList.clear();

        PoolStatistics oldStatistics = this.getStatistics();
        this.poolStatistics = new PoolStatistics();

        this.startPool();

        return oldStatistics;
    }

    public void shutdown() {
        if (this.watchThread != null) {
            this.watchThread.interrupt();
            this.watchThread = null;
        }

        for (PooledConnection pooledConnection : this.availableList) {
            try {
                pooledConnection.rawConnection.close();
            } catch (SQLException exc) {
            }
        }

        for (PooledConnection pooledConnection : this.loanedList) {
            try {
                pooledConnection.rawConnection.close();
            } catch (SQLException exc) {
            }
        }

        this.availableList.clear();
        this.loanedList.clear();
    }
}