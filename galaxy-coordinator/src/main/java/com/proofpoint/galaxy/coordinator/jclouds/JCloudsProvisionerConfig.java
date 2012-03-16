package com.proofpoint.galaxy.coordinator.jclouds;

import com.proofpoint.configuration.Config;
import com.proofpoint.configuration.ConfigDescription;

import javax.validation.constraints.NotNull;

public class JCloudsProvisionerConfig
{
    private String endpoint;
    private String identity;
    private String credential;
    private String provider;
    private String defaultImageId;
    private Integer jcloudsAgentDefaultPort;
    private String defaultInstanceType;

    @NotNull
    public String getProvider()
    {
        return provider;
    }

    @Config("coordinator.jclouds.provider")
    @ConfigDescription("The jclouds provider type of the compute service to communicate with.")
    public JCloudsProvisionerConfig setProvider(String provider)
    {
        this.provider = provider;
        return this;
    }

    @NotNull
    public String getEndpoint()
    {
        return endpoint;
    }

    @Config("coordinator.jclouds.endpoint")
    @ConfigDescription("The endpoint to use for the compute service with jclouds.")
    public JCloudsProvisionerConfig setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    @NotNull
    public String getIdentity()
    {
        return identity;
    }

    @Config("coordinator.jclouds.identity")
    @ConfigDescription("The identity to use when communicating with the compute service through jclouds.")
    public JCloudsProvisionerConfig setIdentity(String identity)
    {
        this.identity = identity;
        return this;
    }

    @NotNull
    public String getCredential()
    {
        return credential;
    }

    @Config("coordinator.jclouds.credential")
    @ConfigDescription("The credential to use when communicating with the compute service through jclouds.")
    public JCloudsProvisionerConfig setCredential(String credential)
    {
        this.credential = credential;
        return this;
    }

    @NotNull
    public String getDefaultImageId()
    {
        return defaultImageId;
    }

    @Config("coordinator.jclouds.image-id")
    @ConfigDescription("The id of the image to use when creating new instances when using the compute service through jclouds.")
    public JCloudsProvisionerConfig setDefaultImageId(String defaultImageId)
    {
        this.defaultImageId = defaultImageId;
        return this;
    }

    @NotNull
    public String getDefaultInstanceType()
    {
        return defaultInstanceType;
    }

    @Config("coordinator.jclouds.default-instance-type")
    @ConfigDescription("The id of the default instance type to use when creating new instances when using the compute service through jclouds.")
    public JCloudsProvisionerConfig setDefaultInstanceType(String defaultInstanceType)
    {
        this.defaultInstanceType = defaultInstanceType;
        return this;
    }

    @Config("coordinator.jclouds.agent.default-port")
    @ConfigDescription("jclouds default port for provisioned agents")
    public JCloudsProvisionerConfig setJcloudsAgentDefaultPort(int jcloudsAgentDefaultPort)
    {
        this.jcloudsAgentDefaultPort = jcloudsAgentDefaultPort;
        return this;
    }

    @NotNull
    public int getJcloudsAgentDefaultPort()
    {
        return jcloudsAgentDefaultPort;
    }
}
