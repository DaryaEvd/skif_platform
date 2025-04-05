package ru.nsu.fit.evdokimova.supervisor.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

// todo: use it later maybe not now
public class GitLabModelLoader implements IModelLoader {
    private static final String GITLAB_REPO_URL = "https://gitlab.com/"; // todo: write gitlab url later

    @Override
    public String loadModel(String modelName, String version) {
        String repoUrl = GITLAB_REPO_URL + modelName + ".git";
        String localPath = "/home/darya/models/" + modelName + "_" + version; // todo: to constants

        try {
            if (!Files.exists(Path.of(localPath))) {
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(new File(localPath))
                        .call();
            }
            return localPath;
        } catch (GitAPIException e) {
            throw new RuntimeException("Error when loading data from gitLab: " + e.getMessage(), e);
        }
    }
}
