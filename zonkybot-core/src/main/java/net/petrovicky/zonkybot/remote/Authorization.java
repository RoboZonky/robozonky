package net.petrovicky.zonkybot.remote;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/oauth")
public interface Authorization {

    @POST
    @Path("token")
    Token login(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("grant_type") @DefaultValue("password") String grantType,
            @FormParam("scope") @DefaultValue("SCOPE_APP_WEB") String scope);

}
