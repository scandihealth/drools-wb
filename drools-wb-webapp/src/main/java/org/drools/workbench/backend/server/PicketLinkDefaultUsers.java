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

@ApplicationScoped
public class PicketLinkDefaultUsers {

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
//        final User director = new User( "director" );
//        final User user = new User( "user" );
//        final User guest = new User( "guest" );
        final User dxc = new User( "dxc" );

        identityManager.add( sds );
//        identityManager.add( director );
//        identityManager.add( user );
//        identityManager.add( guest );
        identityManager.add( dxc );

        identityManager.updateCredential( sds, new Password( "zkOWAc" ) );
//        identityManager.updateCredential( director, new Password( "director" ) );
//        identityManager.updateCredential( user, new Password( "user" ) );
//        identityManager.updateCredential( guest, new Password( "guest" ) );
        identityManager.updateCredential( dxc, new Password( "cwTedqt" ) );

        final Role roleAdmin = new Role( "admin" );
        final Role roleAnalyst = new Role( "analyst" );
        final Role roleKieMgmt = new Role( "kiemgmt" );

        identityManager.add( roleAdmin );
        identityManager.add( roleAnalyst );
        identityManager.add( roleKieMgmt );

        relationshipManager.add( new Grant( sds, roleAnalyst ) );
        relationshipManager.add( new Grant( sds, roleAdmin ) );
        relationshipManager.add( new Grant( sds, roleKieMgmt ) );

//        relationshipManager.add( new Grant( director, roleAnalyst ) );

//        relationshipManager.add( new Grant( user, roleAnalyst ) );

        relationshipManager.add( new Grant( dxc, roleAnalyst ) );
        relationshipManager.add( new Grant( dxc, roleAdmin ) );
        relationshipManager.add( new Grant( dxc, roleKieMgmt ) );
    }

}
