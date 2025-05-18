package ru.nsu.fit.evdokimova.supervisor.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitLabWatcherService {

    private static final String GITLAB_API_BASE = "https://gitlab.com/api/v4/projects/";
    private static final String GITLAB_URL_BASE = "https://gitlab.com/";
    private static final String LOCAL_BASE_DIR = "/home/darya/skif_platform/development/supervisor/models";

    private final Map<String, String> lastKnownCommits = new HashMap<>();

    private final RestTemplate restTemplate = new RestTemplate();

    private final String[] repositories = {
            "daryaevd/c_model"
    };

    @Scheduled(fixedRate = 300_000)
    public void checkForUpdates() {
        for (String repoFullName : repositories) {
            try {
                String latestCommit = fetchLatestCommit(repoFullName);
                if (latestCommit == null) continue;

                String repoName = extractRepoName(repoFullName);
                String lastCommit = lastKnownCommits.get(repoName);

                if (lastCommit == null) {
                    lastKnownCommits.put(repoName, latestCommit);
                    cloneRepo(repoFullName);
                    continue;
                }

                if (!latestCommit.equals(lastCommit)) {
                    System.out.println("Changes detected in " + repoName + ". Updating...");
                    updateRepo(repoFullName);
                    lastKnownCommits.put(repoName, latestCommit);
                } else {
                    System.out.println("No changes in " + repoName + ".");
                }

            } catch (Exception e) {
                System.err.println("Error checking repo " + repoFullName + ": " + e.getMessage());
            }
        }
    }

    private String extractRepoName(String fullName) {
        return fullName.substring(fullName.lastIndexOf('/') + 1);
    }

    private String fetchLatestCommit(String repoFullName) {
        try {
            String encodedRepo = repoFullName.replace("/", "%2F");
            String apiUrl = GITLAB_API_BASE + encodedRepo + "/repository/commits/main";
            Map response = restTemplate.getForObject(apiUrl, Map.class);
            return (String) response.get("id");
        } catch (Exception e) {
            System.err.println("Failed to fetch commit for " + repoFullName + ": " + e.getMessage());
            return null;
        }
    }

    private void cloneRepo(String repoFullName) throws GitAPIException {
        String repoName = extractRepoName(repoFullName);
        File targetDir = new File(LOCAL_BASE_DIR, repoName);
        if (targetDir.exists()) {
            System.out.println("Repo " + repoName + " already cloned.");
            return;
        }
        Git.cloneRepository()
                .setURI(GITLAB_URL_BASE + repoFullName + ".git")
                .setDirectory(targetDir)
                .call();
        System.out.println("Cloned " + repoFullName + " to " + targetDir.getAbsolutePath());
    }

    private void updateRepo(String repoFullName) throws Exception {
        String repoName = extractRepoName(repoFullName);
        File dir = new File(LOCAL_BASE_DIR, repoName);
        if (!dir.exists()) {
            cloneRepo(repoFullName);
            return;
        }
        try (Git git = Git.open(dir)) {
            git.pull().call();
            System.out.println("Updated " + repoName + " via git pull.");
        }
    }

    public void forceCloneRepo(String repoFullName) throws GitAPIException {
        cloneRepo(repoFullName);
    }
}
