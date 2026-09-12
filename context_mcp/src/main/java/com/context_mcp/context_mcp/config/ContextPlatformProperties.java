package com.context_mcp.context_mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Strongly typed configuration for the context platform.
 * Mapped from the {@code context-platform.*} property namespace.
 */
@Validated
@ConfigurationProperties(prefix = "context-platform")
public class ContextPlatformProperties {

    private final Server server = new Server();
    private final Checkpoint checkpoint = new Checkpoint();
    private final Persistence persistence = new Persistence();
    private final Search search = new Search();
    private final Embeddings embeddings = new Embeddings();
    private final Classifier classifier = new Classifier();
    private final Artifacts artifacts = new Artifacts();
    private final Security security = new Security();

    public Server getServer() { return server; }
    public Checkpoint getCheckpoint() { return checkpoint; }
    public Persistence getPersistence() { return persistence; }
    public Search getSearch() { return search; }
    public Embeddings getEmbeddings() { return embeddings; }
    public Classifier getClassifier() { return classifier; }
    public Artifacts getArtifacts() { return artifacts; }
    public Security getSecurity() { return security; }

    public static class Server {
        private boolean localOnly = true;
        public boolean isLocalOnly() { return localOnly; }
        public void setLocalOnly(boolean localOnly) { this.localOnly = localOnly; }
    }

    public static class Checkpoint {
        private boolean enabled = true;
        private int defaultTurnThreshold = 10;
        private List<Integer> supportedThresholds = List.of(5, 10, 20);
        private boolean requireUserConfirmation = true;
        private List<String> autoSaveCategories = List.of();
        private boolean persistRawTranscriptByDefault = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDefaultTurnThreshold() { return defaultTurnThreshold; }
        public void setDefaultTurnThreshold(int defaultTurnThreshold) { this.defaultTurnThreshold = defaultTurnThreshold; }
        public List<Integer> getSupportedThresholds() { return supportedThresholds; }
        public void setSupportedThresholds(List<Integer> supportedThresholds) { this.supportedThresholds = supportedThresholds; }
        public boolean isRequireUserConfirmation() { return requireUserConfirmation; }
        public void setRequireUserConfirmation(boolean requireUserConfirmation) { this.requireUserConfirmation = requireUserConfirmation; }
        public List<String> getAutoSaveCategories() { return autoSaveCategories; }
        public void setAutoSaveCategories(List<String> autoSaveCategories) { this.autoSaveCategories = autoSaveCategories; }
        public boolean isPersistRawTranscriptByDefault() { return persistRawTranscriptByDefault; }
        public void setPersistRawTranscriptByDefault(boolean persistRawTranscriptByDefault) { this.persistRawTranscriptByDefault = persistRawTranscriptByDefault; }
    }

    public static class Persistence {
        private boolean storeRawTranscript = false;
        private Duration softDeleteRetention = Duration.ofDays(30);

        public boolean isStoreRawTranscript() { return storeRawTranscript; }
        public void setStoreRawTranscript(boolean storeRawTranscript) { this.storeRawTranscript = storeRawTranscript; }
        public Duration getSoftDeleteRetention() { return softDeleteRetention; }
        public void setSoftDeleteRetention(Duration softDeleteRetention) { this.softDeleteRetention = softDeleteRetention; }
    }

    public static class Search {
        private boolean semanticEnabled = true;
        private boolean keywordEnabled = true;
        private int defaultLimit = 10;
        private int maxLimit = 100;

        public boolean isSemanticEnabled() { return semanticEnabled; }
        public void setSemanticEnabled(boolean semanticEnabled) { this.semanticEnabled = semanticEnabled; }
        public boolean isKeywordEnabled() { return keywordEnabled; }
        public void setKeywordEnabled(boolean keywordEnabled) { this.keywordEnabled = keywordEnabled; }
        public int getDefaultLimit() { return defaultLimit; }
        public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
        public int getMaxLimit() { return maxLimit; }
        public void setMaxLimit(int maxLimit) { this.maxLimit = maxLimit; }
    }

    public static class Embeddings {
        private String provider = "none";
        private String model = "";
        private int dimensions = 768;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    }

    public static class Classifier {
        private String provider = "rules";
        private boolean remoteAccessEnabled = false;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public boolean isRemoteAccessEnabled() { return remoteAccessEnabled; }
        public void setRemoteAccessEnabled(boolean remoteAccessEnabled) { this.remoteAccessEnabled = remoteAccessEnabled; }
    }

    public static class Artifacts {
        private boolean watcherEnabled = false;
        private List<String> roots = List.of();
        private int maxFileSizeMb = 250;
        private List<String> excludedGlobs = List.of();

        public boolean isWatcherEnabled() { return watcherEnabled; }
        public void setWatcherEnabled(boolean watcherEnabled) { this.watcherEnabled = watcherEnabled; }
        public List<String> getRoots() { return roots; }
        public void setRoots(List<String> roots) { this.roots = roots; }
        public int getMaxFileSizeMb() { return maxFileSizeMb; }
        public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
        public List<String> getExcludedGlobs() { return excludedGlobs; }
        public void setExcludedGlobs(List<String> excludedGlobs) { this.excludedGlobs = excludedGlobs; }
    }

    public static class Security {
        private boolean redactSecrets = true;
        private boolean rejectSecretBearingArtifacts = true;
        private boolean apiTokenEnabled = false;
        private Duration suppressionDefaultDuration = Duration.ofDays(30);

        public boolean isRedactSecrets() { return redactSecrets; }
        public void setRedactSecrets(boolean redactSecrets) { this.redactSecrets = redactSecrets; }
        public boolean isRejectSecretBearingArtifacts() { return rejectSecretBearingArtifacts; }
        public void setRejectSecretBearingArtifacts(boolean rejectSecretBearingArtifacts) { this.rejectSecretBearingArtifacts = rejectSecretBearingArtifacts; }
        public boolean isApiTokenEnabled() { return apiTokenEnabled; }
        public void setApiTokenEnabled(boolean apiTokenEnabled) { this.apiTokenEnabled = apiTokenEnabled; }
        public Duration getSuppressionDefaultDuration() { return suppressionDefaultDuration; }
        public void setSuppressionDefaultDuration(Duration suppressionDefaultDuration) { this.suppressionDefaultDuration = suppressionDefaultDuration; }
    }
}
