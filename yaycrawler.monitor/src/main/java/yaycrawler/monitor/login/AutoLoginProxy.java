package yaycrawler.monitor.login;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import yaycrawler.common.model.PhantomCookie;
import yaycrawler.common.utils.CasperjsProgramManager;
import yaycrawler.common.utils.UrlUtils;
import yaycrawler.dao.domain.SiteAccount;
import yaycrawler.dao.repositories.SiteAccountRepository;

import java.util.List;
import java.util.UUID;

/**
 * Created by ucs_yuananyun on 2016/6/23.
 */
@Component
public class AutoLoginProxy {

    private static Logger logger = LoggerFactory.getLogger(AutoLoginProxy.class);

    @Value("${server.address}")
    private String serverIP;
    @Value("${server.port}")
    private String serverPort;
    @Value("${server.context-path}")
    private String serverContextPath;

    @Autowired
    private SiteAccountRepository siteAccountRepository;


    public LoginResult login(String pageUrl, String loginJsFileName, String pageContent) {
        if (StringUtils.isBlank(loginJsFileName)) {
            logger.error("jsFileName不能为空！");
            return null;
        }

        SiteAccount siteAccount = siteAccountRepository.findOneByDomain(UrlUtils.getDomain(pageUrl));
        String userName = siteAccount.getUserName();
        String password = siteAccount.getPassWord();

        if (StringUtils.isBlank(pageContent)) return null;
        if (StringUtils.isBlank(userName)) return null;
        if (StringUtils.isBlank(password)) return null;

        String resolverAddress = String.format("http://%s:%s%s/%s", serverIP, serverPort, serverContextPath, "resolveGeetestSlicePosition");
        int i = 0;
        String result = null;
        LoginResult loginResult = new LoginResult();
        while (i++ < 5) {
            result = CasperjsProgramManager.launch(loginJsFileName, pageUrl, resolverAddress, UUID.randomUUID().toString(), userName, password, " --web-security=no", "--ignore-ssl-errors=true");
            logger.info(result);
            if (result.contains("自动登录成功")) {
                String cookie = StringUtils.substringBetween(result, "$CookieStart", "$CookieEnd");
                if (StringUtils.isNotEmpty(cookie)) {
                    List<PhantomCookie> phantomCookies = JSON.parseArray(cookie, PhantomCookie.class);
                    loginResult.setCookies(phantomCookies);
                    loginResult.setSuccess(true);
                    break;
                }
            } else {
                loginResult.setSuccess(false);
            }
        }
        return loginResult;
    }
}