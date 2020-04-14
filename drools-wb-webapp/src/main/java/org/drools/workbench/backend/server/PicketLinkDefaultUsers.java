/**
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.workbench.backend.server;

import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.picketlink.authentication.event.PreAuthenticateEvent;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.Grant;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PicketLinkDefaultUsers {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private PartitionManager partitionManager;

    private final AtomicBoolean hasInitialized = new AtomicBoolean( false );

    public void onPreAuthenticateEvent( @Observes PreAuthenticateEvent event ) {
        if ( !hasInitialized.getAndSet( true ) ) {
            setup();
        }
    }

    private void setup() {
        final IdentityManager identityManager = partitionManager.createIdentityManager();
        final RelationshipManager relationshipManager = partitionManager.createRelationshipManager();

        final User sds = new User( "sds" );
        final User erv = new User( "erv" );
        final User krni = new User( "krni" );
//        final User director = new User( "director" );
//        final User user = new User( "user" );
//        final User guest = new User( "guest" );
        final User dxc = new User( "dxc" );

        identityManager.add( sds );
        identityManager.add( erv );
        identityManager.add( krni );
//        identityManager.add( director );
//        identityManager.add( user );
//        identityManager.add( guest );
        identityManager.add( dxc );

        Properties passwords = getPasswords();

        identityManager.updateCredential(sds, new Password( passwords.getProperty("sds") ) );
        identityManager.updateCredential(erv, new Password( passwords.getProperty("erv") ) );
        identityManager.updateCredential(krni, new Password( passwords.getProperty("krni") ) );
        identityManager.updateCredential(dxc, new Password( passwords.getProperty("dxc") ) );

        final Role roleAdmin = new Role( "admin" );
        final Role roleAnalyst = new Role( "analyst" );
        final Role roleKieMgmt = new Role( "kiemgmt" );

        identityManager.add( roleAdmin );
        identityManager.add( roleAnalyst );
        identityManager.add( roleKieMgmt );

        relationshipManager.add( new Grant( sds, roleAnalyst ) );
        relationshipManager.add( new Grant( sds, roleAdmin ) );
        relationshipManager.add( new Grant( sds, roleKieMgmt ) );

        relationshipManager.add( new Grant( erv, roleAnalyst ) );
        relationshipManager.add( new Grant( erv, roleAdmin ) );
        relationshipManager.add( new Grant( erv, roleKieMgmt ) );

        relationshipManager.add( new Grant( krni, roleAnalyst ) );
        relationshipManager.add( new Grant( krni, roleAdmin ) );
        relationshipManager.add( new Grant( krni, roleKieMgmt ) );

//        relationshipManager.add( new Grant( director, roleAnalyst ) );

//        relationshipManager.add( new Grant( user, roleAnalyst ) );

        relationshipManager.add( new Grant( dxc, roleAnalyst ) );
        relationshipManager.add( new Grant( dxc, roleAdmin ) );
        relationshipManager.add( new Grant( dxc, roleKieMgmt ) );
    }

    private Properties getPasswords() {
        Properties properties = new Properties();
        String fileName = System.getProperty("jboss.server.config.dir") + "/users.properties";
        try {
            FileInputStream fis = new FileInputStream(fileName);
            if(fis != null) {
                properties.load(fis);
                fis.close();
            }
        } catch(Exception e) {
            logger.error("Error read user passwords from property file", e);
        }
        return properties;
    }
}
