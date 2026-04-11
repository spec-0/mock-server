package io.spec0.mockserver.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Caches request bodies for {@code /mock/**} so validation and CEL can read JSON without consuming
 * the input stream twice.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class MockRequestContentCacheFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (uri != null && uri.contains("/mock/")) {
      ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
      filterChain.doFilter(wrapped, response);
    } else {
      filterChain.doFilter(request, response);
    }
  }
}
