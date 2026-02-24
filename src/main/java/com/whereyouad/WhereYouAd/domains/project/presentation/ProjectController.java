package com.whereyouad.WhereYouAd.domains.project.presentation;

import com.whereyouad.WhereYouAd.domains.project.presentation.docs.ProjectControllerDocs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/project")
public class ProjectController implements ProjectControllerDocs {
}
