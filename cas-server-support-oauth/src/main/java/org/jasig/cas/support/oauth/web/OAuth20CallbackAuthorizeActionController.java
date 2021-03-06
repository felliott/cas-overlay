/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.support.oauth.web;

import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.authentication.principal.SimpleWebApplicationServiceImpl;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.support.oauth.OAuthConstants;
import org.jasig.cas.support.oauth.OAuthUtils;
import org.jasig.cas.support.oauth.services.OAuthRegisteredService;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.util.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This controller is called if a user selects an action to allow or deny
 * authorization.
 *
 * @author Michael Haselton
 * @since 4.1.0
 */
public final class OAuth20CallbackAuthorizeActionController extends AbstractController {

    private final Logger LOGGER = LoggerFactory.getLogger(OAuth20CallbackAuthorizeActionController.class);

    private final ServicesManager servicesManager;

    private final CentralAuthenticationService centralAuthenticationService;

    private final CipherExecutor cipherExecutor;

    public OAuth20CallbackAuthorizeActionController(final ServicesManager servicesManager,
                                                    final CentralAuthenticationService centralAuthenticationService,
                                                    final CipherExecutor cipherExecutor) {
        this.servicesManager = servicesManager;
        this.centralAuthenticationService = centralAuthenticationService;
        this.cipherExecutor = cipherExecutor;
    }

    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        final HttpSession session = request.getSession();

        // get action
        final String action = request.getParameter(OAuthConstants.OAUTH20_APPROVAL_PROMPT_ACTION);
        LOGGER.debug("{} : {}", OAuthConstants.OAUTH20_APPROVAL_PROMPT_ACTION, action);

        if (!action.equalsIgnoreCase(OAuthConstants.OAUTH20_APPROVAL_PROMPT_ACTION_ALLOW)) {
            LOGGER.error("{} was not allowed.", OAuthConstants.OAUTH20_APPROVAL_PROMPT_ACTION);
            // callback url with error
            String callbackUrl = (String) session.getAttribute(OAuthConstants.OAUTH20_CALLBACKURL);
            callbackUrl = OAuthUtils.addParameter(callbackUrl, OAuthConstants.ERROR, OAuthConstants.ACCESS_DENIED);
            LOGGER.debug("{} : {}", OAuthConstants.OAUTH20_CALLBACKURL, callbackUrl);
            return OAuthUtils.redirectTo(callbackUrl);
        }

        // retrieve client id from session
        String clientId = (String) session.getAttribute(OAuthConstants.OAUTH20_CLIENT_ID);
        LOGGER.debug("{} : {}", OAuthConstants.OAUTH20_CLIENT_ID, clientId);
        session.removeAttribute(OAuthConstants.OAUTH20_CLIENT_ID);
        if (StringUtils.isBlank(clientId)) {
            LOGGER.error("{} is missing from the session and can not be retrieved.", OAuthConstants.OAUTH20_CLIENT_ID);
            return new ModelAndView(OAuthConstants.ERROR_VIEW);
        }

        // retrieve callback url from session
        String callbackUrl = (String) session.getAttribute(OAuthConstants.OAUTH20_CALLBACKURL);
        LOGGER.debug("{} : {}", OAuthConstants.OAUTH20_CALLBACKURL, callbackUrl);
        session.removeAttribute(OAuthConstants.OAUTH20_CALLBACKURL);
        if (StringUtils.isBlank(callbackUrl)) {
            LOGGER.error("{} is missing from the session and can not be retrieved.", OAuthConstants.OAUTH20_CALLBACKURL);
            return new ModelAndView(OAuthConstants.ERROR_VIEW);
        }

        // retrieve state from session (csrf equivalent)
        final String state = (String) session.getAttribute(OAuthConstants.OAUTH20_STATE);
        LOGGER.debug("{} : {}", OAuthConstants.OAUTH20_STATE, state);
        session.removeAttribute(OAuthConstants.OAUTH20_STATE);
        if (state != null) {
            callbackUrl = OAuthUtils.addParameter(callbackUrl, OAuthConstants.STATE, state);
        }

        String loginTicketId = (String) session.getAttribute(OAuthConstants.OAUTH20_LOGIN_TICKET_ID);
        final OAuthRegisteredService registeredService = OAuthUtils.getRegisteredOAuthService(this.servicesManager, clientId);
        session.removeAttribute(OAuthConstants.OAUTH20_LOGIN_TICKET_ID);
        if (registeredService == null) {
            LOGGER.error("Unknown {} : {}", OAuthConstants.CLIENT_ID, clientId);
            return new ModelAndView(OAuthConstants.ERROR_VIEW);
        }

        Service service = new SimpleWebApplicationServiceImpl(callbackUrl);
        final ServiceTicket serviceTicket = centralAuthenticationService.grantServiceTicket(loginTicketId, service);

        // callback url with code (encrypted service ticket)
        callbackUrl = OAuthUtils.addParameter(callbackUrl, OAuthConstants.CODE, cipherExecutor.encode(serviceTicket.getId()));
        LOGGER.debug("{} : {}", OAuthConstants.OAUTH20_CALLBACKURL, callbackUrl);

        return OAuthUtils.redirectTo(callbackUrl);
    }
}
