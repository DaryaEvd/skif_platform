package ru.nsu.fit.evdokimova.supervisor.controller;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.fit.evdokimova.supervisor.service.GitLabWatcherService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gitlab")
public class GitlabController {
    private final GitLabWatcherService gitLabWatcherService;

    @PostMapping("/check")
    public String checkForUpdates() {
        gitLabWatcherService.checkForUpdates();
        return "Update check triggered.";
    }

    @PostMapping("/clone")
    public String cloneRepo(@RequestParam("repo") String repoFullName) {
        try {
            gitLabWatcherService.forceCloneRepo(repoFullName);
            return "Repository '" + repoFullName + "' cloned (if not already).";
        } catch (GitAPIException e) {
            return "Clone failed for '" + repoFullName + "': " + e.getMessage();
        }
    }
}
