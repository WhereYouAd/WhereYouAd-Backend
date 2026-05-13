package com.whereyouad.WhereYouAd.domains.timeline.presentation;

import com.whereyouad.WhereYouAd.domains.timeline.presentation.docs.TimelineControllerDocs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/timeline")
public class TimelineController implements TimelineControllerDocs {

}
