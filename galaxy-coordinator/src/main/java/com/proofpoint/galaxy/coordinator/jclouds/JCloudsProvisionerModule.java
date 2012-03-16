package com.proofpoint.galaxy.coordinator.jclouds;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.galaxy.coordinator.InMemoryStateManager;
import com.proofpoint.galaxy.coordinator.Provisioner;
import com.proofpoint.galaxy.coordinator.StateManager;
import com.proofpoint.galaxy.coordinator.auth.AuthorizedKey;
import com.proofpoint.galaxy.coordinator.auth.AuthorizedKeyStore;
import com.proofpoint.galaxy.coordinator.auth.InMemoryAuthorizedKeyStore;

import javax.inject.Singleton;
import java.util.Collections;

public class JCloudsProvisionerModule implements Module
{

    @Override
    public void configure(Binder binder)
    {
        binder.disableCircularProxies();
        binder.requireExplicitBindings();

        binder.bind(Provisioner.class).to(JCloudsProvisioner.class).in(Scopes.SINGLETON);
        binder.bind(StateManager.class).to(InMemoryStateManager.class).in(Scopes.SINGLETON);
        ConfigurationModule.bindConfig(binder).to(JCloudsProvisionerConfig.class);
    }

    @Provides
    @Singleton
    public AuthorizedKeyStore getInMemoryAuthorizedKeyStore()
    {
        return new InMemoryAuthorizedKeyStore(Collections.<AuthorizedKey>emptyList());
    }
}
