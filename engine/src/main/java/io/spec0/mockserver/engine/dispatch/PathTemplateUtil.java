package io.spec0.mockserver.engine.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PathTemplateUtil {

  private PathTemplateUtil() {}

  static Map<String, Object> extractPathParams(String template, String path) {
    List<String> paramNames = new ArrayList<>();
    StringBuilder regexBuilder = new StringBuilder("^");
    Pattern varPattern = Pattern.compile("\\{([^/]+)}");
    Matcher templateMatcher = varPattern.matcher(template);
    int lastEnd = 0;

    while (templateMatcher.find()) {
      regexBuilder.append(Pattern.quote(template.substring(lastEnd, templateMatcher.start())));
      paramNames.add(templateMatcher.group(1));
      regexBuilder.append("([^/]+)");
      lastEnd = templateMatcher.end();
    }
    regexBuilder.append(Pattern.quote(template.substring(lastEnd)));
    regexBuilder.append("$");

    Matcher matcher = Pattern.compile(regexBuilder.toString()).matcher(path);
    if (!matcher.matches()) {
      return null;
    }

    Map<String, Object> params = new HashMap<>();
    for (int i = 0; i < paramNames.size(); i++) {
      params.put(paramNames.get(i), matcher.group(i + 1));
    }
    return params;
  }
}
