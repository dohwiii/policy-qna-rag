package com.company.policyqna.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @GetMapping({"/", "/chat"})
    public String chat() {
        return "chat"; // → /WEB-INF/views/chat.jsp
    }
}
