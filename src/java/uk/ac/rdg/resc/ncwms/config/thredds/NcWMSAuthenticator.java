/*
 * ncWMSAuthenticator.java
 *
 * Created on 18 July 2007, 13:47
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.config.thredds;


import sun.misc.BASE64Encoder;
import java.net.PasswordAuthentication;
import java.net.Authenticator;

/**
* Proxy Authentication.  
**/
public class NcWMSAuthenticator extends Authenticator 
{
    private String proxy_userName;
    private String proxy_password;
    private String webAuth_userName;
    private String webAuth_password;
    private String proxy_host;
    private String proxy_port;

    public NcWMSAuthenticator()
   {
        proxy_userName = null;
        proxy_password = null;
        proxy_host = null;
        proxy_port = null;
    }

    public void setProxy(String _proxy_userName, String _proxy_password, String _proxy_host, String _proxy_port)
    {
        proxy_password = _proxy_password;
        proxy_userName = _proxy_userName;
        proxy_host = _proxy_host;
        proxy_port = _proxy_port;

        try
        {
                System.setProperty("proxySet","true");
                System.setProperty("http.proxyHost", proxy_host);
                System.setProperty("http.proxyPort", proxy_port);
        }
        catch(IllegalStateException ie)
        {
                //report.log(3, "IllegalStateException: " + ie.toString());
        }
        catch(NullPointerException  nullpointer)
        {
                //report.log(3, "Set proxy key is null");
        }

    }

    public void setWebAuth(String _webAuth_userName, String _webAuth_password)
    {
        webAuth_userName = _webAuth_userName;
        webAuth_password = _webAuth_password;
    }

    protected PasswordAuthentication getPasswordAuthentication()
    {
        //check the proxy port to be the same as the one specified in .spider_rc
        //we can't check the host with getRequestingHost() because the host in .spider_rc
        //may not always be the same as the requesting host.  E.g. proxy.utas.edu.au
        //will request from proxy1.utas.edu.au.
        if((proxy_port != null) && (this.getRequestingPort() == Integer.parseInt(proxy_port)))
        {
            return new PasswordAuthentication(proxy_userName, proxy_password.toCharArray());
        }
        else if((this.getRequestingScheme().equalsIgnoreCase("basic")) || (this.getRequestingScheme().equalsIgnoreCase("digest")))
        {
            return new PasswordAuthentication(webAuth_userName, webAuth_password.toCharArray());
        }
        return null;
    }
} 

