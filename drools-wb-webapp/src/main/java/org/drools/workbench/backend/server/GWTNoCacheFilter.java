package org.drools.workbench.backend.server;

import java.io.IOException;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link Filter} that add cache control headers for GWT generated "nocache" file to ensure that the file never gets cached. <br/>
 * This is necessary because GWT 2.7 has a bug that affects gwt-maven-plugin 2.7's staleness check, which means the server will always respond that the file has not changed. <br/>
 * <p>
 * See also:<br/>
 * https://github.com/gwtproject/gwt/issues/9108 <br/>
 * https://scandihealth.atlassian.net/browse/LPR-1394 <br/>
 * https://seewah.blogspot.dk/2009/02/gwt-tips-2-nocachejs-getting-cached-in.html
 */
public class GWTNoCacheFilter implements Filter {
    @Override
    public void init( FilterConfig filterConfig ) {
    }

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain filterChain ) throws IOException, ServletException {
        HttpServletRequest httpRequest = ( HttpServletRequest ) request;
        String requestURI = httpRequest.getRequestURI();

        if ( requestURI.contains( ".nocache." ) ) {
            Date now = new Date();
            HttpServletResponse httpResponse = ( HttpServletResponse ) response;
            httpResponse.setDateHeader( "Date", now.getTime() );
            // one day old
            httpResponse.setDateHeader( "Expires", now.getTime() - 86400000L );
            httpResponse.setHeader( "Pragma", "no-cache" );
            httpResponse.setHeader( "Cache-control", "no-cache, no-store, must-revalidate" );
        }
        filterChain.doFilter( request, response );
    }

    @Override
    public void destroy() {
    }
}
