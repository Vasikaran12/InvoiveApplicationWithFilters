import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JsonFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws
            IOException, ServletException{
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse) response;

        if(req.getAttribute("reqJson") == null && (req.getMethod()
                .equals("POST") || req.getMethod().equals("PUT"))){
            new Util().sendResponse(res.getWriter(), new Response(false, new Error(400, "Invalid request format")),
                    (HttpServletResponse) res);
        }else{
            filterChain.doFilter(req, res);
        }
    }

    @Override
    public void destroy(){
        Filter.super.destroy();
    }
}