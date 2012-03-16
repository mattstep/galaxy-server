package com.proofpoint.galaxy.coordinator.jclouds;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Module;
import com.proofpoint.galaxy.coordinator.CoordinatorConfig;
import com.proofpoint.galaxy.coordinator.Instance;
import com.proofpoint.galaxy.coordinator.Provisioner;
import com.proofpoint.galaxy.shared.Repository;
import com.proofpoint.node.NodeInfo;
import org.jclouds.Constants;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;
import org.jclouds.openstack.keystone.v2_0.config.CredentialType;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;

public class JCloudsProvisioner implements Provisioner
{

    private final ComputeService computeService;
    private final String environment;
    private final Integer galaxyPort;
    private final Image defaultImage;
    private final String defaultInstanceType;
    private final Repository repository;

    @Inject
    public JCloudsProvisioner(JCloudsProvisionerConfig config, NodeInfo nodeInfo, Repository repository, CoordinatorConfig coordinatorConfig)
    {
        Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_ENDPOINT, config.getEndpoint());
        overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialType.PASSWORD_CREDENTIALS.toString());
        overrides.setProperty(KeystoneProperties.VERSION, "2.0");

        Set<Module> moduleOverrides = ImmutableSet.<Module>of(new JCloudsLoggingAdapterModule());

        ComputeServiceContext context = new ComputeServiceContextFactory().createContext(
                config.getProvider(),
                config.getIdentity(),
                config.getCredential(),
                moduleOverrides,
                overrides);

        computeService = context.getComputeService();

        environment = nodeInfo.getEnvironment();
        galaxyPort = config.getJcloudsAgentDefaultPort();
        defaultInstanceType = config.getDefaultInstanceType();

        defaultImage = getImageForName(config.getDefaultImageId());

        this.repository = repository;

        Preconditions.checkNotNull(computeService);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(galaxyPort);
        Preconditions.checkNotNull(defaultImage);
        Preconditions.checkNotNull(defaultInstanceType);
    }

    private Image getImageForName(final String defaultImageName)
    {
        return Iterables.find(computeService.listImages(), new Predicate<Image>()
        {
            @Override
            public boolean apply(@Nullable Image image)
            {
                return image.getName().equals(defaultImageName);
            }
        });
    }

    @Override
    public List<Instance> listCoordinators()
    {
        Iterable<? extends NodeMetadata> runningCoordinatorNodes =
                computeService.listNodesDetailsMatching(new OnlyRunningInThisEnvironmentPredicate("coordinator"));

        return ImmutableList.copyOf(transform(runningCoordinatorNodes, new NodeMetadataToInstance()));
    }

    @Override
    public List<Instance> provisionCoordinators(String coordinatorConfigSpec,
            int coordinatorCount,
            String instanceType,
            String availabilityZone,
            String ami,
            String keyPair,
            String securityGroup)
    {
        instanceType = Objects.firstNonNull(instanceType, defaultInstanceType);

        final Set<? extends NodeMetadata> newNodes;
        try {
            newNodes = computeService.createNodesInGroup("galaxy-" + environment, coordinatorCount, new GalaxyTemplate(instanceType, availabilityZone, ami, "coordinator"));
        }
        catch (RunNodesException e) {
            throw Throwables.propagate(e);
        }

        return ImmutableList.copyOf(transform(newNodes, new NodeMetadataToInstance()));
    }

    @Override
    public List<Instance> listAgents()
    {
        Iterable<? extends NodeMetadata> runningAgentNodes =
                computeService.listNodesDetailsMatching(new OnlyRunningInThisEnvironmentPredicate("agent"));

        return ImmutableList.copyOf(transform(runningAgentNodes, new NodeMetadataToInstance()));
    }

    @Override
    public List<Instance> provisionAgents(String agentConfigSpec,
            int agentCount,
            String instanceType,
            String availabilityZone,
            String ami,
            String keyPair,
            String securityGroup)
    {
        instanceType = Objects.firstNonNull(instanceType, defaultInstanceType);

        final Set<? extends NodeMetadata> newNodes;
        try {
            newNodes = computeService.createNodesInGroup("galaxy-" + environment, agentCount, new GalaxyTemplate(instanceType, availabilityZone, ami, "agent"));
        }
        catch (RunNodesException e) {
            throw Throwables.propagate(e);
        }

        return ImmutableList.copyOf(transform(newNodes, new NodeMetadataToInstance()));
    }

    @Override
    public void terminateAgents(final List<String> instanceIds)
    {
        for (String instanceId : instanceIds) {
            computeService.destroyNode(instanceId);
        }
    }

    private class NodeMetadataToInstance implements Function<NodeMetadata, Instance>
    {
        @Override
        public Instance apply(@Nullable NodeMetadata nodeMetadata)
        {
            Location location = nodeMetadata.getLocation();

            URI internalGalaxyAgentUri = URI.create(String.format("http://%s:%s",
                            nodeMetadata.getPrivateAddresses().iterator().next(),
                            nodeMetadata.getUserMetadata().get("galaxy:port")));

            URI externalGalaxyAgentUri = nodeMetadata.getPublicAddresses().isEmpty() ?
                    internalGalaxyAgentUri :
                    URI.create(String.format("http://%s:%s",
                            nodeMetadata.getPublicAddresses().iterator().next(),
                            nodeMetadata.getUserMetadata().get("galaxy:port")));

            return new Instance(nodeMetadata.getId(),
                    nodeMetadata.getName(),
                    location.getDescription(),
                    internalGalaxyAgentUri,
                    externalGalaxyAgentUri);
        }
    }

    private class OnlyRunningInThisEnvironmentPredicate implements Predicate<ComputeMetadata>
    {
        private final String galaxyType;

        public OnlyRunningInThisEnvironmentPredicate(String galaxyType)
        {
            this.galaxyType = galaxyType;
        }
        @Override
        public boolean apply(@Nullable ComputeMetadata computeMetadata)
        {
            if (!computeMetadata.getUserMetadata().get("galaxy:role").equals(galaxyType)) {
                return false;
            }

            if (!computeMetadata.getUserMetadata().get("galaxy:environment").equals(environment)) {
                return false;
            }

            NodeMetadata nodeMetadata = computeService.getNodeMetadata(computeMetadata.getId());

            return nodeMetadata.getState().equals(NodeState.RUNNING);
        }
    }

    private class GalaxyTemplate implements Template
    {

        private final Location location;
        private final Hardware hardware;
        private final Image image;
        private final String galaxyType;

        public GalaxyTemplate(final String instanceType, final String availabilityZone, final String imageName, String galaxyType)
        {
            if (availabilityZone == null) {
                location = computeService.listAssignableLocations().iterator().next();
            }
            else {
                location = Iterables.find(computeService.listAssignableLocations(), new Predicate<Location>()
                {
                    @Override
                    public boolean apply(@Nullable Location location)
                    {
                        return location.getId().equals(availabilityZone);
                    }
                });
            }

            hardware = Iterables.find(computeService.listHardwareProfiles(), new Predicate<Hardware>()
            {
                @Override
                public boolean apply(@Nullable Hardware hardware)
                {
                    return hardware.getName().equals(instanceType);
                }
            });

            image = getImageForName(imageName);

            this.galaxyType = galaxyType;
        }

        @Override
        public Image getImage()
        {
            return image;
        }

        @Override
        public Hardware getHardware()
        {
            return hardware;
        }

        @Override
        public Location getLocation()
        {
            return location;
        }

        @Override
        public TemplateOptions getOptions()
        {
            TemplateOptions options = new TemplateOptions();
            options.runScript("apt-get install openjdk-6-jdk; apt-get install ruby1.9");
            options.userMetadata(ImmutableMap.<String, String>of("galaxy:role", galaxyType, "galaxy:environment", environment, "galaxy:port", String.valueOf(galaxyPort)));
            return options;
        }
    }
}
