package net.dertod2.DatabaseHandler.Database.Pooler;

import java.util.concurrent.TimeUnit;

public class PoolSettings {
    protected String jdbcUrl;

    protected String username;
    protected String password;

    protected int minAvailable = 2;

    protected int minPoolSize = 2;
    protected int maxPoolSize = 50;

    protected long startSleepMode = TimeUnit.MINUTES.toMillis(20);

    protected long maxLifetime = TimeUnit.MINUTES.toMillis(60);
    protected long maxIdletime = TimeUnit.MINUTES.toMillis(10);
    protected long maxLoanedTime = TimeUnit.MINUTES.toMillis(5); // Auto-Kills the Connection when not retuned to the
                                                                 // pool

    protected long watcherTimer = TimeUnit.SECONDS.toMillis(10);

    public void setUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets how much Connections must be free in the Pool to grab
     * 
     * @param minAvailable
     */
    public void setMinimumAvailable(int minAvailable) {
        this.minAvailable = minAvailable;
    }

    public int getMinimumAvailable() {
        return this.minAvailable;
    }

    public void setMinimumPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMinimumPoolSize() {
        return this.minPoolSize;
    }

    public void setMaximumPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaximumPoolSize() {
        return this.maxPoolSize;
    }

    /**
     * When should the pool stops to open idle connections ?<br />
     * This means: long time no connections fetched -> no connections pooled until
     * one connection will be fetched
     * 
     * @param timeToStartinMS
     */
    public void setStartSleepMode(long timeToStartinMS) {
        this.startSleepMode = timeToStartinMS;
    }

    public long getStartSleepMode() {
        return this.startSleepMode;
    }

    public void setLifetime(long maxlifeTimeMS) {
        this.maxLifetime = maxlifeTimeMS;
    }

    public long getLifetime() {
        return this.maxLifetime;
    }

    public void setIdletime(long maxIdletimeMS) {
        this.maxIdletime = maxIdletimeMS;
    }

    public long getIdletime() {
        return this.maxIdletime;
    }

    public void setLoanedTime(long maxLoanedtimeMS) {
        this.maxLoanedTime = maxLoanedtimeMS;
    }

    public long getLoanedTime() {
        return this.maxLoanedTime;
    }

    public void setWatcherTime(long watcherTimeMS) {
        this.watcherTimer = watcherTimeMS;
    }

    public long getWatcherTime() {
        return this.watcherTimer;
    }
}