package fr.aplose.erp.core.web;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

@Controller
@RequestMapping("/docs")
public class DocsController {

    private static final String DOCS_BASE = "docs";
    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    @GetMapping
    public String index() {
        return "redirect:/docs/user";
    }

    @GetMapping("/user")
    public String userIndex(Model model) {
        return renderDoc(model, "user", "index", List.of(
                DocLink.of("index", "docs.user.index", "user"),
                DocLink.of("getting-started", "docs.user.gettingStarted", "user"),
                DocLink.of("crm", "docs.user.crm", "user"),
                DocLink.of("commerce", "docs.user.commerce", "user"),
                DocLink.of("catalog", "docs.user.catalog", "user"),
                DocLink.of("projects", "docs.user.projects", "user")
        ));
    }

    @GetMapping("/user/{page}")
    public String userPage(@PathVariable String page, Model model) {
        return renderDoc(model, "user", page, List.of(
                DocLink.of("index", "docs.user.index", "user"),
                DocLink.of("getting-started", "docs.user.gettingStarted", "user"),
                DocLink.of("crm", "docs.user.crm", "user"),
                DocLink.of("commerce", "docs.user.commerce", "user"),
                DocLink.of("catalog", "docs.user.catalog", "user"),
                DocLink.of("projects", "docs.user.projects", "user")
        ));
    }

    @GetMapping("/integrator")
    public String integratorIndex(Model model) {
        return renderDoc(model, "integrator", "index", List.of(
                DocLink.of("index", "docs.integrator.index", "integrator"),
                DocLink.of("api", "docs.integrator.api", "integrator")
        ));
    }

    @GetMapping("/integrator/{page}")
    public String integratorPage(@PathVariable String page, Model model) {
        return renderDoc(model, "integrator", page, List.of(
                DocLink.of("index", "docs.integrator.index", "integrator"),
                DocLink.of("api", "docs.integrator.api", "integrator")
        ));
    }

    @GetMapping("/developer")
    public String developerIndex(Model model) {
        return renderDoc(model, "developer", "index", List.of(
                DocLink.of("index", "docs.developer.index", "developer"),
                DocLink.of("architecture", "docs.developer.architecture", "developer"),
                DocLink.of("modules", "docs.developer.modules", "developer")
        ));
    }

    @GetMapping("/developer/{page}")
    public String developerPage(@PathVariable String page, Model model) {
        return renderDoc(model, "developer", page, List.of(
                DocLink.of("index", "docs.developer.index", "developer"),
                DocLink.of("architecture", "docs.developer.architecture", "developer"),
                DocLink.of("modules", "docs.developer.modules", "developer")
        ));
    }

    private String renderDoc(Model model, String section, String page, List<DocLink> toc) {
        String path = DOCS_BASE + "/" + section + "/" + sanitizePage(page) + ".md";
        String content = loadMarkdown(path);
        model.addAttribute("docContent", content != null ? content : "<p>Documentation à venir.</p>");
        model.addAttribute("docSection", section);
        model.addAttribute("docPage", page);
        model.addAttribute("toc", toc);
        return "public/docs";
    }

    private String loadMarkdown(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) return null;
            try (InputStream is = resource.getInputStream(); Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                String raw = s.hasNext() ? s.next() : "";
                Node node = MARKDOWN_PARSER.parse(raw);
                return HTML_RENDERER.render(node);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitizePage(String page) {
        if (page == null || page.isBlank()) return "index";
        return page.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    /** path is the segment after /docs/ e.g. "user" or "user/getting-started" for building URLs */
    public record DocLink(String page, String labelKey, String path) {
        static DocLink of(String page, String labelKey, String section) {
            String path = "index".equals(page) ? section : section + "/" + page;
            return new DocLink(page, labelKey, path);
        }
    }
}
