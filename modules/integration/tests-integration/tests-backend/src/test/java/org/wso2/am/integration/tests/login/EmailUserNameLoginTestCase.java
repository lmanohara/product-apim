/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.am.admin.clients.rest.api.WorkFlowAdminRestClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.testng.Assert.assertEquals;

/**
 * test case to test login using email user name. test is done for publisher,
 * store and admin-dashboard. modified api manager configurations can be found in
 * configFiles/emailusernametest location
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class EmailUserNameLoginTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private WorkFlowAdminRestClient workflowAdmin;
    private static final Log log = LogFactory.getLog(EmailUserNameLoginTestCase.class);
    private ServerConfigurationManager serverConfigurationManager ;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        String workflowAdminURLHTTP = getStoreURLHttp();

        String apiManagerXml =
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "emailusernametest" +
                        File.separator + "api-manager.xml";

        String userMgtXml =
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "emailusernametest" +
                        File.separator + "user-mgt.xml";

        String carbonXml =
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "emailusernametest" +
                        File.separator + "carbon.xml";

        configureServer(apiManagerXml, userMgtXml, carbonXml);

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
        workflowAdmin = new WorkFlowAdminRestClient(workflowAdminURLHTTP);

    }

    @Test(groups = {"wso2.am"}, description = "Email username login test case")
    public void LoginWithEmailUserNameTestCase() throws Exception {

        String userNameWithEmail = "emailuser@email.com";
        String password = "email123";
        String domainName = "emailuserdomain.com";
        String fullUserName = userNameWithEmail + "@" + domainName;
        boolean isSuccessful =
                createTenantWithEmailUserName(userNameWithEmail, password,
                        domainName, publisherContext.getContextUrls().getBackEndUrl());
        assertEquals(isSuccessful, true);

        HttpResponse login;

        // check for publisher login with email user name
        login = apiPublisher.login(fullUserName, password);
        assertEquals(login.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Login to Publisher with email username failed");
        // check for store login with email user name
        login = apiStore.login(fullUserName, password);
        assertEquals(login.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Login to Store with email username failed");
        // check for Admin Portal login with email user name
        login = workflowAdmin.login(fullUserName, password);
        assertEquals(login.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Login to Admin Portal Login to Publisher with email username failed");
    }

    /**
     * create a new tenant with email address as the user name.
     *
     * @param userNameWithEmail tenant name with email
     * @param pwd               tenant password
     * @param domainName        tenant domain name
     * @param backendUrl        apim backend url
     * @return boolean whether tenant creation was successful or not
     */
    private boolean createTenantWithEmailUserName(String userNameWithEmail, String pwd,
                                                  String domainName, String backendUrl)
            throws XPathExpressionException, RemoteException, TenantMgtAdminServiceExceptionException {
        boolean isSuccess = false;

        String endPoint = backendUrl + "TenantMgtAdminService";
        TenantMgtAdminServiceStub tenantMgtAdminServiceStub =
                new TenantMgtAdminServiceStub(
                        endPoint);
        AuthenticateStub.authenticateStub(publisherContext.getSuperTenant().getContextUser().getUserName(),
                publisherContext.getSuperTenant().getContextUser().getUserName(), tenantMgtAdminServiceStub);

        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        tenantInfoBean.setActive(true);
        tenantInfoBean.setEmail(userNameWithEmail);
        tenantInfoBean.setAdminPassword(pwd);
        tenantInfoBean.setAdmin(userNameWithEmail);
        tenantInfoBean.setTenantDomain(domainName);
        tenantInfoBean.setCreatedDate(calendar);
        tenantInfoBean.setFirstname("emailuser first name");
        tenantInfoBean.setLastname("email user last name");
        tenantInfoBean.setSuccessKey("true");
        tenantInfoBean.setUsagePlan("demo");

        TenantInfoBean tenantInfoBeanGet;

        tenantInfoBeanGet = tenantMgtAdminServiceStub.getTenant(domainName);

        if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() != 0) {
            tenantMgtAdminServiceStub.activateTenant(domainName);
            log.info("Tenant domain " + domainName + " Activated successfully");

        } else if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() == 0) {
            tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
            tenantMgtAdminServiceStub.activateTenant(domainName);
            log.info("Tenant domain " + domainName +
                    " created and activated successfully");
            log.info("Tenant domain " + domainName + " created and activated successfully");
            isSuccess = true;
        } else {
            log.info("Tenant domain " + domainName + " already registered");
        }
        return isSuccess;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
        super.cleanUp();
    }

    private void configureServer(String apiManagerXml, String userMgtXml, String carbonXml) throws Exception {
        try {
            serverConfigurationManager = new ServerConfigurationManager(publisherContext);
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(apiManagerXml));
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(userMgtXml));
            serverConfigurationManager.applyConfiguration(new File(carbonXml));
        }catch (Exception e){
            throw new APIManagerIntegrationTestException("Error while changing server configuration", e);
        }
    }

}
