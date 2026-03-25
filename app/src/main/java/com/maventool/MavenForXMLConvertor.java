package com.maventool;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
public class MavenForXMLConvertor {

        // ==================== 对象 → XML ====================
        public static Document projectToXml(MavenProject project) throws ParserConfigurationException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("project");
            root.setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
            doc.appendChild(root);

            // 添加基本元素
            addElement(doc, root, "modelVersion", project.getModelVersion());
            addElement(doc, root, "groupId", project.getGroupId());
            addElement(doc, root, "artifactId", project.getArtifactId());
            addElement(doc, root, "version", project.getVersion());
            addElement(doc, root, "packaging", project.getPackaging());
            addElement(doc, root, "name", project.getName());
            addElement(doc, root, "description", project.getDescription());
            addElement(doc, root, "url", project.getUrl());

            // Licenses
            if (project.getLicenses() != null && !project.getLicenses().isEmpty()) {
                Element licensesElem = doc.createElement("licenses");
                for (License license : project.getLicenses()) {
                    Element licenseElem = doc.createElement("license");
                    addElement(doc, licenseElem, "name", license.getName());
                    addElement(doc, licenseElem, "url", license.getUrl());
                    licensesElem.appendChild(licenseElem);
                }
                root.appendChild(licensesElem);
            }

            // Developers
            if (project.getDevelopers() != null && !project.getDevelopers().isEmpty()) {
                Element developersElem = doc.createElement("developers");
                for (Developer dev : project.getDevelopers()) {
                    Element devElem = doc.createElement("developer");
                    addElement(doc, devElem, "name", dev.getName());
                    addElement(doc, devElem, "email", dev.getEmail());
                    developersElem.appendChild(devElem);
                }
                root.appendChild(developersElem);
            }

            // Scm
            if (project.getScm() != null) {
                Element scmElem = doc.createElement("scm");
                addElement(doc, scmElem, "connection", project.getScm().getConnection());
                addElement(doc, scmElem, "developerConnection", project.getScm().getDeveloperConnection());
                addElement(doc, scmElem, "url", project.getScm().getUrl());
                root.appendChild(scmElem);
            }

            // Properties
            if (project.getProperties() != null && !project.getProperties().isEmpty()) {
                Element propsElem = doc.createElement("properties");
                for (Map.Entry<String, String> entry : project.getProperties().entrySet()) {
                    addElement(doc, propsElem, entry.getKey(), entry.getValue());
                }
                root.appendChild(propsElem);
            }

            // Dependencies
            if (project.getDependencies() != null && !project.getDependencies().isEmpty()) {
                Element depsElem = doc.createElement("dependencies");
                for (Dependency dep : project.getDependencies()) {
                    Element depElem = doc.createElement("dependency");
                    addElement(doc, depElem, "groupId", dep.getGroupId());
                    addElement(doc, depElem, "artifactId", dep.getArtifactId());
                    addElement(doc, depElem, "version", dep.getVersion());
                    addElement(doc, depElem, "scope", dep.getScope());

                    // Exclusions
                    if (dep.getExclusions() != null && !dep.getExclusions().isEmpty()) {
                        Element exclElem = doc.createElement("exclusions");
                        for (Exclusion excl : dep.getExclusions()) {
                            Element e = doc.createElement("exclusion");
                            addElement(doc, e, "groupId", excl.getGroupId());
                            addElement(doc, e, "artifactId", excl.getArtifactId());
                            exclElem.appendChild(e);
                        }
                        depElem.appendChild(exclElem);
                    }
                    depsElem.appendChild(depElem);
                }
                root.appendChild(depsElem);
            }

            // Build
            if (project.getBuild() != null) {
                Element buildElem = doc.createElement("build");
                Build build = project.getBuild();

                // Resources
                if (build.getResources() != null && !build.getResources().isEmpty()) {
                    Element resourcesElem = doc.createElement("resources");
                    for (Resource res : build.getResources()) {
                        Element resElem = doc.createElement("resource");
                        addElement(doc, resElem, "directory", res.getDirectory());
                        if (res.getIncludes() != null && !res.getIncludes().isEmpty()) {
                            Element includesElem = doc.createElement("includes");
                            for (String inc : res.getIncludes()) {
                                addElement(doc, includesElem, "include", inc);
                            }
                            resElem.appendChild(includesElem);
                        }
                        addElement(doc, resElem, "filtering", Boolean.toString(res.isFiltering()));
                        resourcesElem.appendChild(resElem);
                    }
                    buildElem.appendChild(resourcesElem);
                }

                // Plugins
                if (build.getPlugins() != null && !build.getPlugins().isEmpty()) {
                    Element pluginsElem = doc.createElement("plugins");
                    for (Plugin plugin : build.getPlugins()) {
                        Element pluginElem = doc.createElement("plugin");
                        addElement(doc, pluginElem, "groupId", plugin.getGroupId());
                        addElement(doc, pluginElem, "artifactId", plugin.getArtifactId());
                        addElement(doc, pluginElem, "version", plugin.getVersion());

                        // Configuration
                        if (plugin.getConfiguration() != null && plugin.getConfiguration().getProperties() != null) {
                            Element configElem = doc.createElement("configuration");
                            for (Map.Entry<String, Object> entry : plugin.getConfiguration().getProperties().entrySet()) {
                                Element child = doc.createElement(entry.getKey());
                                child.setTextContent(entry.getValue().toString());
                                configElem.appendChild(child);
                            }
                            pluginElem.appendChild(configElem);
                        }

                        // Executions
                        if (plugin.getExecutions() != null && !plugin.getExecutions().isEmpty()) {
                            Element execsElem = doc.createElement("executions");
                            for (Execution exec : plugin.getExecutions()) {
                                Element execElem = doc.createElement("execution");
                                addElement(doc, execElem, "id", exec.getId());
                                addElement(doc, execElem, "phase", exec.getPhase());
                                if (exec.getGoals() != null && !exec.getGoals().isEmpty()) {
                                    Element goalsElem = doc.createElement("goals");
                                    for (String goal : exec.getGoals()) {
                                        addElement(doc, goalsElem, "goal", goal);
                                    }
                                    execElem.appendChild(goalsElem);
                                }
                                // Execution configuration
                                if (exec.getConfiguration() != null && exec.getConfiguration().getProperties() != null) {
                                    Element execConfig = doc.createElement("configuration");
                                    for (Map.Entry<String, Object> entry : exec.getConfiguration().getProperties().entrySet()) {
                                        Element child = doc.createElement(entry.getKey());
                                        child.setTextContent(entry.getValue().toString());
                                        execConfig.appendChild(child);
                                    }
                                    execElem.appendChild(execConfig);
                                }
                                execsElem.appendChild(execElem);
                            }
                            pluginElem.appendChild(execsElem);
                        }

                        pluginsElem.appendChild(pluginElem);
                    }
                    buildElem.appendChild(pluginsElem);
                }

                root.appendChild(buildElem);
            }

            // DistributionManagement
            if (project.getDistributionManagement() != null) {
                Element distElem = doc.createElement("distributionManagement");
                DistributionManagement dm = project.getDistributionManagement();
                if (dm.getRepository() != null) {
                    Element repoElem = doc.createElement("repository");
                    addElement(doc, repoElem, "id", dm.getRepository().getId());
                    addElement(doc, repoElem, "name", dm.getRepository().getName());
                    addElement(doc, repoElem, "url", dm.getRepository().getUrl());
                    distElem.appendChild(repoElem);
                }
                if (dm.getSnapshotRepository() != null) {
                    Element snapElem = doc.createElement("snapshotRepository");
                    addElement(doc, snapElem, "id", dm.getSnapshotRepository().getId());
                    addElement(doc, snapElem, "name", dm.getSnapshotRepository().getName());
                    addElement(doc, snapElem, "url", dm.getSnapshotRepository().getUrl());
                    distElem.appendChild(snapElem);
                }
                root.appendChild(distElem);
            }

            // Profiles
            if (project.getProfiles() != null && !project.getProfiles().isEmpty()) {
                Element profilesElem = doc.createElement("profiles");
                for (Profile profile : project.getProfiles()) {
                    Element profileElem = doc.createElement("profile");
                    addElement(doc, profileElem, "id", profile.getId());
                    if (profile.getBuild() != null) {
                        // 简单处理，实际需递归，此处简化
                    }
                    profilesElem.appendChild(profileElem);
                }
                root.appendChild(profilesElem);
            }

            return doc;
        }

        // 辅助方法：创建文本节点元素
        private static void addElement(Document doc, Element parent, String tag, String text) {
            if (text == null) return;
            Element elem = doc.createElement(tag);
            elem.appendChild(doc.createTextNode(text));
            parent.appendChild(elem);
        }

        // ==================== XML → 对象 ====================
        public static MavenProject xmlToProject(Document doc) {
            Element root = doc.getDocumentElement();
            MavenProject project = new MavenProject();

            project.setModelVersion(getElementText(root, "modelVersion"));
            project.setGroupId(getElementText(root, "groupId"));
            project.setArtifactId(getElementText(root, "artifactId"));
            project.setVersion(getElementText(root, "version"));
            project.setPackaging(getElementText(root, "packaging"));
            project.setName(getElementText(root, "name"));
            project.setDescription(getElementText(root, "description"));
            project.setUrl(getElementText(root, "url"));

            // Licenses
            Element licensesElem = getChildElement(root, "licenses");
            if (licensesElem != null) {
                List<License> licenses = new ArrayList<>();
                NodeList licenseNodes = licensesElem.getElementsByTagName("license");
                for (int i = 0; i < licenseNodes.getLength(); i++) {
                    Element licenseElem = (Element) licenseNodes.item(i);
                    License lic = new License();
                    lic.setName(getElementText(licenseElem, "name"));
                    lic.setUrl(getElementText(licenseElem, "url"));
                    licenses.add(lic);
                }
                project.setLicenses(licenses);
            }

            // Developers
            Element devsElem = getChildElement(root, "developers");
            if (devsElem != null) {
                List<Developer> developers = new ArrayList<>();
                NodeList devNodes = devsElem.getElementsByTagName("developer");
                for (int i = 0; i < devNodes.getLength(); i++) {
                    Element devElem = (Element) devNodes.item(i);
                    Developer dev = new Developer();
                    dev.setName(getElementText(devElem, "name"));
                    dev.setEmail(getElementText(devElem, "email"));
                    developers.add(dev);
                }
                project.setDevelopers(developers);
            }

            // Scm
            Element scmElem = getChildElement(root, "scm");
            if (scmElem != null) {
                Scm scm = new Scm();
                scm.setConnection(getElementText(scmElem, "connection"));
                scm.setDeveloperConnection(getElementText(scmElem, "developerConnection"));
                scm.setUrl(getElementText(scmElem, "url"));
                project.setScm(scm);
            }

            // Properties
            Element propsElem = getChildElement(root, "properties");
            if (propsElem != null) {
                Map<String, String> props = new HashMap<>();
                NodeList children = propsElem.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        props.put(node.getNodeName(), node.getTextContent());
                    }
                }
                project.setProperties(props);
            }

            // Dependencies
            Element depsElem = getChildElement(root, "dependencies");
            if (depsElem != null) {
                List<Dependency> deps = new ArrayList<>();
                NodeList depNodes = depsElem.getElementsByTagName("dependency");
                for (int i = 0; i < depNodes.getLength(); i++) {
                    Element depElem = (Element) depNodes.item(i);
                    Dependency dep = new Dependency();
                    dep.setGroupId(getElementText(depElem, "groupId"));
                    dep.setArtifactId(getElementText(depElem, "artifactId"));
                    dep.setVersion(getElementText(depElem, "version"));
                    dep.setScope(getElementText(depElem, "scope"));
                    // Exclusions
                    Element exclElem = getChildElement(depElem, "exclusions");
                    if (exclElem != null) {
                        List<Exclusion> exclusions = new ArrayList<>();
                        NodeList exclNodes = exclElem.getElementsByTagName("exclusion");
                        for (int j = 0; j < exclNodes.getLength(); j++) {
                            Element e = (Element) exclNodes.item(j);
                            Exclusion excl = new Exclusion();
                            excl.setGroupId(getElementText(e, "groupId"));
                            excl.setArtifactId(getElementText(e, "artifactId"));
                            exclusions.add(excl);
                        }
                        dep.setExclusions(exclusions);
                    }
                    deps.add(dep);
                }
                project.setDependencies(deps);
            }

            // Build
            Element buildElem = getChildElement(root, "build");
            if (buildElem != null) {
                Build build = new Build();

                // Resources
                Element resourcesElem = getChildElement(buildElem, "resources");
                if (resourcesElem != null) {
                    List<Resource> resources = new ArrayList<>();
                    NodeList resNodes = resourcesElem.getElementsByTagName("resource");
                    for (int i = 0; i < resNodes.getLength(); i++) {
                        Element resElem = (Element) resNodes.item(i);
                        Resource res = new Resource();
                        res.setDirectory(getElementText(resElem, "directory"));
                        // Includes
                        Element includesElem = getChildElement(resElem, "includes");
                        if (includesElem != null) {
                            List<String> includes = new ArrayList<>();
                            NodeList incNodes = includesElem.getElementsByTagName("include");
                            for (int j = 0; j < incNodes.getLength(); j++) {
                                includes.add(incNodes.item(j).getTextContent());
                            }
                            res.setIncludes(includes);
                        }
                        res.setFiltering(Boolean.parseBoolean(getElementText(resElem, "filtering")));
                        resources.add(res);
                    }
                    build.setResources(resources);
                }

                // Plugins
                Element pluginsElem = getChildElement(buildElem, "plugins");
                if (pluginsElem != null) {
                    List<Plugin> plugins = new ArrayList<>();
                    NodeList pluginNodes = pluginsElem.getElementsByTagName("plugin");
                    for (int i = 0; i < pluginNodes.getLength(); i++) {
                        Element pluginElem = (Element) pluginNodes.item(i);
                        Plugin plugin = new Plugin();
                        plugin.setGroupId(getElementText(pluginElem, "groupId"));
                        plugin.setArtifactId(getElementText(pluginElem, "artifactId"));
                        plugin.setVersion(getElementText(pluginElem, "version"));

                        // Configuration
                        Element configElem = getChildElement(pluginElem, "configuration");
                        if (configElem != null) {
                            Configuration config = new Configuration();
                            Map<String, Object> cfgMap = new HashMap<>();
                            NodeList configChildren = configElem.getChildNodes();
                            for (int j = 0; j < configChildren.getLength(); j++) {
                                Node node = configChildren.item(j);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    cfgMap.put(node.getNodeName(), node.getTextContent());
                                }
                            }
                            config.setProperties(cfgMap);
                            plugin.setConfiguration(config);
                        }

                        // Executions
                        Element execsElem = getChildElement(pluginElem, "executions");
                        if (execsElem != null) {
                            List<Execution> executions = new ArrayList<>();
                            NodeList execNodes = execsElem.getElementsByTagName("execution");
                            for (int j = 0; j < execNodes.getLength(); j++) {
                                Element execElem = (Element) execNodes.item(j);
                                Execution exec = new Execution();
                                exec.setId(getElementText(execElem, "id"));
                                exec.setPhase(getElementText(execElem, "phase"));
                                // Goals
                                Element goalsElem = getChildElement(execElem, "goals");
                                if (goalsElem != null) {
                                    List<String> goals = new ArrayList<>();
                                    NodeList goalNodes = goalsElem.getElementsByTagName("goal");
                                    for (int k = 0; k < goalNodes.getLength(); k++) {
                                        goals.add(goalNodes.item(k).getTextContent());
                                    }
                                    exec.setGoals(goals);
                                }
                                // Execution configuration
                                Element execConfigElem = getChildElement(execElem, "configuration");
                                if (execConfigElem != null) {
                                    Configuration execConfig = new Configuration();
                                    Map<String, Object> cfgMapExec = new HashMap<>();
                                    NodeList configChildrenExec = execConfigElem.getChildNodes();
                                    for (int k = 0; k < configChildrenExec.getLength(); k++) {
                                        Node node = configChildrenExec.item(k);
                                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                                            cfgMapExec.put(node.getNodeName(), node.getTextContent());
                                        }
                                    }
                                    execConfig.setProperties(cfgMapExec);
                                    exec.setConfiguration(execConfig);
                                }
                                executions.add(exec);
                            }
                            plugin.setExecutions(executions);
                        }

                        plugins.add(plugin);
                    }
                    build.setPlugins(plugins);
                }
                project.setBuild(build);
            }

            // DistributionManagement
            Element distElem = getChildElement(root, "distributionManagement");
            if (distElem != null) {
                DistributionManagement dm = new DistributionManagement();
                Element repoElem = getChildElement(distElem, "repository");
                if (repoElem != null) {
                    Repository repo = new Repository();
                    repo.setId(getElementText(repoElem, "id"));
                    repo.setName(getElementText(repoElem, "name"));
                    repo.setUrl(getElementText(repoElem, "url"));
                    dm.setRepository(repo);
                }
                Element snapRepoElem = getChildElement(distElem, "snapshotRepository");
                if (snapRepoElem != null) {
                    Repository snapRepo = new Repository();
                    snapRepo.setId(getElementText(snapRepoElem, "id"));
                    snapRepo.setName(getElementText(snapRepoElem, "name"));
                    snapRepo.setUrl(getElementText(snapRepoElem, "url"));
                    dm.setSnapshotRepository(snapRepo);
                }
                project.setDistributionManagement(dm);
            }

            // Profiles
            Element profilesElem = getChildElement(root, "profiles");
            if (profilesElem != null) {
                List<Profile> profiles = new ArrayList<>();
                NodeList profileNodes = profilesElem.getElementsByTagName("profile");
                for (int i = 0; i < profileNodes.getLength(); i++) {
                    Element profileElem = (Element) profileNodes.item(i);
                    Profile profile = new Profile();
                    profile.setId(getElementText(profileElem, "id"));
                    // 可继续解析 build 等，此处简化
                    profiles.add(profile);
                }
                project.setProfiles(profiles);
            }

            return project;
        }

        private static Element getChildElement(Element parent, String tagName) {
            NodeList list = parent.getElementsByTagName(tagName);
            return list.getLength() > 0 ? (Element) list.item(0) : null;
        }

        private static String getElementText(Element parent, String tagName) {
            Element child = getChildElement(parent, tagName);
            return child != null ? child.getTextContent() : null;
        }

        // ==================== 文件操作 ====================
        public static void saveToFile(Document doc, String filePath) throws TransformerException, IOException {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                StreamResult result = new StreamResult(fos);
                transformer.transform(source, result);
            }
        }

        public static Document loadFromFile(String filePath) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new File(filePath));
        }
    }

