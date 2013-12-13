package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientHandlerException
import com.sun.jersey.api.client.ClientRequest
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.api.client.filter.ClientFilter
import com.sun.jersey.api.client.filter.LoggingFilter
import com.sun.jersey.api.representation.Form
import org.apache.log4j.Logger;

class RightscaleAPIRequest implements RightscaleAPI {
    static Logger logger = Logger.getLogger(RightscaleAPIRequest.class);

    def String email
    def String password
    def String account
    def String endpoint

    def RestClient restClient;
    def boolean authenticated = false;
    /**
     * Default constructor.
     */
    RightscaleAPIRequest(String email, String password, String account, String endpoint) {
        this.email = email
        this.password = password
        this.account = account
        this.endpoint = endpoint

        restClient = new RestClient(endpoint)
    }

    public void initialize() {
        restClient.authenticate()
        authenticated = true;
    }

    /**
     * Query API service for the Servers
     * @return Map mof models keyed by server href.
     */
    @Override
    public Map<String, RightscaleResource> getServers() {
        def Node xml = restClient.get("/api/servers.xml", [view: "instance_detail"])
        System.out.println("DEBUG: number of servers in response to GET /api/servers: " + xml.server.size())

        def servers = ServerResource.burst(xml, 'server', ServerResource.&create)
        System.out.println("DEBUG: number of servers from burst: " + servers.size())

        return servers
    }

    /**
     * Query API service for a list of instances for the cloud.
     * @param cloud_id
     * @return Map of models keyed by cloud href.
     */
    @Override
    public Map<String, RightscaleResource> getInstances(String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/instances", [view: "extended"])
        return InstanceResource.burst(xml, 'instance', InstanceResource.&create)
    }

    /**
     * Query Rightscale API service for all the deployments.
     * @return Map of Deployments keyed by their href
     */
    @Override
    public Map<String, RightscaleResource> getDeployments() {
        def Node xml = restClient.get("/api/deployments.xml", [:])
        return DeploymentResource.burst(xml, 'deployment', DeploymentResource.&create)
    }

    /**
     * Query the Rightscale API for the Cloud resources.
     * @return Map of Clouds
     */
    @Override
    public Map<String, RightscaleResource> getClouds() {
        def Node xml = restClient.get("/api/clouds.xml", [:])
        return CloudResource.burst(xml, 'cloud', CloudResource.&create)
    }

    /**
     * Query the Rightscale API for all ServerTemplate resources.
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getServerTemplates() {
        def Node xml = restClient.get('/api/server_templates.xml', [:])
        return ServerTemplateResource.burst(xml, 'server_template', ServerTemplateResource.&create)
    }

    /**
     * Query the Rightscale API for all Datacenter resources for the specified cloud.
     * @param cloud_id
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getDatacenters(String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/datacenters.xml", [:])
        return DatacenterResource.burst(xml, 'datacenter', DatacenterResource.&create)
    }

    /**
     * Query the Rightscale API for a single Subnet resource.
     * @return Map of Subnets
     */
    @Override
    public Map<String, RightscaleResource> getSubnets(final String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/subnets", [:])
        return SubnetResource.burst(xml, 'subnet', SubnetResource.&create)
    }

    /**
     * Query the Rightscale API for a single SshKey resource.
     * @return Map of SshKeys
     */
    @Override
    public Map<String, RightscaleResource> getSshKeys(final String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/ssh_keys", [:])
        return SshKeyResource.burst(xml, 'ssh_key', SshKeyResource.&create)
    }

    /**
     * Query the Rightscale API for Image resources.
     * @return Map of Images
     */
    @Override
    public Map<String, RightscaleResource> getImages(final String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/images", [:])
        return ImageResource.burst(xml, 'image', ImageResource.&create)
    }
    /**
     * Query the Rightscale API for a single Image resource.
     * @return Map of Images
     */
    @Override
    public RightscaleResource getImage(final String href) {
        def Node xml = restClient.get(href, [:])
        return ImageResource.create(xml)
    }

    /**
     * Query the Rightscale API for all ResourceInput resources for the specified instance.
     * @param cloud_id
     * @param instance_id
     * @return List of maps containing name/value pairs.
     */
    @Override
    public Map<String, RightscaleResource> getInputs(final String cloud_id, String instance_id) {
        def String href = "/api/clouds/${cloud_id}/instances/${instance_id}/inputs"
        def Node xml = restClient.get(href, [:])
        return InputResource.burst(xml, 'input', InputResource.&create)
    }

    /**
     * Query the Rightscale API for all InstanceType resources for the specified cloud.
     * @param cloud_id
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getInstanceTypes(String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/instance_types.xml", [:])
        return InstanceTypeResource.burst(xml, 'instance_type', InstanceTypeResource.&create)
    }

    /**
     * Query the Rightscale API for all ServerArray resources.
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getServerArrays() {
        def Node xml = restClient.get("/api/server_arrays", [view: "instance_detail"])
        return ServerArrayResource.burst(xml, 'server_array', ServerArrayResource.&create)
    }

    @Override
    public Map<String, RightscaleResource> getServerArrayInstances(String server_array_id) {
        def Node xml = restClient.get("/api/server_arrays/${server_array_id}/current_instances",[:])
        return InstanceResource.burst(xml, 'instance', InstanceResource.&create)
    }

    /**
     * Query the Rightscale API for the tags assigned to the specified resource.
     * @return Map of Tags
     */
    @Override
    public Map<String, RightscaleResource> getTags(final String href) {
        def request = '/api/tags/by_resource.xml' as Rest;
        request.addFilter(new LoggingFilter(System.out)); // debug output
        def ClientResponse response = request.post({}, [:], ["resource_hrefs[]": href]);
        if (response.status != 200) {
            throw new ResourceModelSourceException("RightScale ${href} tags request error. " + response)
        }
        return TagsResource.burst(response.XML, 'resource_tag', TagsResource.&create)
    }




    /**
     * Helper class for making Rightscale API HTTP requests.
     */
    class RestClient {

        private ArrayList<Object> cookies;
        private String baseUrl

        RestClient(baseUrl) {
            // API defaults
            Rest.defaultHeaders = ["X-API-VERSION": "1.5"]
            this.baseUrl = baseUrl
            Rest.baseUrl = this.baseUrl;
            addSendCookieFilter(Rest.client)
        }

        /**
         * Adds a filter to the jersey client that will send all of the stored cookies in each request.
         * @param client
         */
        private void addSendCookieFilter(Client client) {
            client.addFilter(new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                    if (cookies != null) {
                        request.getHeaders().put("Cookie", cookies);
                    }
                    return getNext().handle(request);
                }
            });
        }
/**
         * Login and create a session.
         */
        private void authenticate() {
            //use a local Jersey client
            def client = Client.create()
            //add a filter to store and send all cookies received from the server
            client.addFilter(new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                    if (cookies != null) {
                        request.getHeaders().put("Cookie", cookies);
                    }
                    ClientResponse response = getNext().handle(request);
                    // copy cookies
                    if (response.getCookies() != null) {
                        if (cookies == null) {
                            cookies = new ArrayList<Object>();
                        }
                        cookies.addAll(response.getCookies());
                    }
                    return response;
                }
            });

            WebResource sessionRequest = client.resource("${endpoint}/api/session");
            Form form = new Form();
            form.putSingle("email", email);
            form.putSingle("password", password);
            form.putSingle("account_href", "/api/accounts/${account}");
            def ClientResponse response = sessionRequest.header("X-API-VERSION", "1.5")
                    .type("application/x-www-form-urlencoded").post(ClientResponse.class, form);
            /**
             * Check the response for http status (eg, 20x)
             */
            if (response.status != 204) {
                cookies == null
                throw new ResourceModelSourceException("RightScale login error. " + response)
            }
        }

        /**
         * Get the resource
         * @param href
         * @param params
         * @return Node containing the resource as XML data.
         */
        Node get(String href, Map params) {
            if (null == href) throw new IllegalAccessException("href cannot be null")

            System.out.println("DEBUG: RightscaleAPIRequest.RestClient: Getting resource by href: ${href}")
            /**
             * Request the servers data as XML
             */
            def request = href as Rest;
            request.addFilter(new LoggingFilter(System.out)); // debug output
            def ClientResponse response = request.get([:], params); // instance_detail contains extra info
            if (response.status != 200) {
                throw new ResourceModelSourceException("RightScale ${href} request error. " + response)
            }

            return response.XML
        }

        Node post(String href, Map params) {
            def request = href as Rest;

            request.addFilter(new LoggingFilter(System.out)); // debug output
            def ClientResponse response = request.post({}, [:], params);
            if (response.status != 200) {
                throw new ResourceModelSourceException("RightScale ${href} request error. " + response)
            }

            return response.XML
        }
    }

}

