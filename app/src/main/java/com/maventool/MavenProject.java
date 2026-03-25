package com.maventool;

import java.util.List;
import java.util.Map;

public class MavenProject {
    private String modelVersion;
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private String name;
    private String description;
    private String url;
    private List<License> licenses;
    private List<Developer> developers;
    private Scm scm;
    private Map<String, String> properties;   // key-value
    private List<Dependency> dependencies;
    private Build build;
    private DistributionManagement distributionManagement;
    private List<Profile> profiles;

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public void setDevelopers(List<Developer> developers) {
        this.developers = developers;
    }

    public void setScm(Scm scm) {
        this.scm = scm;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public void setDistributionManagement(DistributionManagement distributionManagement) {
        this.distributionManagement = distributionManagement;
    }

    public void setProfiles(List<Profile> profiles) {
        this.profiles = profiles;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public List<Developer> getDevelopers() {
        return developers;
    }

    public Scm getScm() {
        return scm;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Build getBuild() {
        return build;
    }

    public DistributionManagement getDistributionManagement() {
        return distributionManagement;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    // getter/setter...
}

class License {
    private String name;
    private String url;
    // getter/setter...

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

class Developer {
    private String name;
    private String email;
    // getter/setter...

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }
}

class Scm {
    private String connection;
    private String developerConnection;
    private String url;
    // getter/setter...

    public String getConnection() {
        return connection;
    }

    public String getDeveloperConnection() {
        return developerConnection;
    }

    public String getUrl() {
        return url;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public void setDeveloperConnection(String developerConnection) {
        this.developerConnection = developerConnection;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

class Dependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private List<Exclusion> exclusions;
    // getter/setter...


    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getScope() {
        return scope;
    }

    public List<Exclusion> getExclusions() {
        return exclusions;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setExclusions(List<Exclusion> exclusions) {
        this.exclusions = exclusions;
    }
}

class Exclusion {
    private String groupId;
    private String artifactId;
    // getter/setter...

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
}

class Build {
    private List<Resource> resources;
    private List<Plugin> plugins;
    // getter/setter...
    public List<Resource> getResources() {
        return resources;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public void setPlugins(List<Plugin> plugins) {
        this.plugins = plugins;
    }
}

class Resource {
    private String directory;
    private List<String> includes;
    private boolean filtering;
    // getter/setter...

    public String getDirectory() {
        return directory;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public boolean isFiltering() {
        return filtering;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public void setFiltering(boolean filtering) {
        this.filtering = filtering;
    }
}

class Plugin {
    private String groupId;
    private String artifactId;
    private String version;
    private Configuration configuration;
    private List<Execution> executions;
    // getter/setter...

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public List<Execution> getExecutions() {
        return executions;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setExecutions(List<Execution> executions) {
        this.executions = executions;
    }
}

class Execution {
private String id;
private String phase;
private List<String> goals;
private Configuration configuration;
// getter/setter...

    public String getId() {
        return id;
    }

    public String getPhase() {
        return phase;
    }

    public List<String> getGoals() {
        return goals;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}

class Configuration {
    private Map<String, Object> properties;
    // getter/setter...

    public Map<String, Object> getProperties() {
        return properties;

    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}

    class DistributionManagement {
    private Repository repository;
    private Repository snapshotRepository;
    // getter/setter...

        public Repository getRepository() {
            return repository;
        }

        public Repository getSnapshotRepository() {
            return snapshotRepository;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }

        public void setSnapshotRepository(Repository snapshotRepository) {
            this.snapshotRepository = snapshotRepository;
        }
    }

class Repository {
    private String id;
    private String name;
    private String url;
    // getter/setter...

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

 class Profile {
    private String id;
    private Build build;
    // 还可添加 activation, properties 等，根据需要扩展

    public String getId() {
        return id;
    }

    public Build getBuild() {
        return build;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBuild(Build build) {
        this.build = build;
    }
}
