package com.blockware.spring.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("__blockware")
public class BlockwareController {

    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping("health")
    public void health(HttpServletResponse response) throws IOException {

        response.setHeader("Content-Type", "application/json");
        response.setStatus(200);

        objectMapper.writeValue(response.getWriter(), new Health());
        response.flushBuffer();
    }

    public static class Health {
        boolean ok = true;

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }
    }
}
