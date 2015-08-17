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
package org.jasig.cas.support.oauth.ticket.support;

import org.jasig.cas.support.oauth.authentication.principal.OAuthCredential;
import org.jasig.cas.support.oauth.authentication.principal.OAuthService;
import org.jasig.cas.ticket.*;
import org.jasig.cas.ticket.support.AbstractCasExpirationPolicy;

import javax.validation.constraints.NotNull;

/**
 * Delegates to different expiration policies depending on whether oauth
 * is true or not.
 *
 * @author Michael Haselton
 * @since 4.1.0
 *
 */
public final class OAuthDelegatingExpirationPolicy extends AbstractCasExpirationPolicy {

    /** Serialization support. */
    private static final long serialVersionUID = 4461752518354198401L;

    @NotNull
    private ExpirationPolicy oAuthExpirationPolicy;

    @NotNull
    private ExpirationPolicy sessionExpirationPolicy;

    @Override
    public boolean isExpired(final TicketState ticketState) {
        final AbstractTicket ticket = (AbstractTicket) ticketState;
        final ServiceTicket serviceTicket = ticket instanceof ServiceTicket ? (ServiceTicket) ticket : null;
        final TicketGrantingTicket ticketGrantingTicket = ticket.getGrantingTicket() != null ? ticket.getGrantingTicket() : (TicketGrantingTicket) ticket;

        final Boolean isAuthenticationOAuth = (Boolean) ticketGrantingTicket.getAuthentication().getAttributes()
                .get(OAuthCredential.AUTHENTICATION_ATTRIBUTE_OAUTH);
        if (isAuthenticationOAuth != null && isAuthenticationOAuth) {
            return this.oAuthExpirationPolicy.isExpired(ticketState);
        }

        if (serviceTicket != null && serviceTicket.getService() instanceof OAuthService) {
            return ticketGrantingTicket.isExpired();
        }

        return this.sessionExpirationPolicy.isExpired(ticketState);
    }

    public void setOAuthExpirationPolicy(final ExpirationPolicy oAuthExpirationPolicy) {
        this.oAuthExpirationPolicy = oAuthExpirationPolicy;
    }

    public void setSessionExpirationPolicy(final ExpirationPolicy sessionExpirationPolicy) {
        this.sessionExpirationPolicy = sessionExpirationPolicy;
    }
}
