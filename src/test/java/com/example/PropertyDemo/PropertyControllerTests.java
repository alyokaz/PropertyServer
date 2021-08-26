package com.example.PropertyDemo;

import com.example.PropertyDemo.Agent.Agent;
import com.example.PropertyDemo.Property.Location;
import com.example.PropertyDemo.Property.Property;
import com.example.PropertyDemo.Property.PropertyType;
import com.example.PropertyDemo.Property.RentalProperty;
import com.example.PropertyDemo.Property.SaleProperty;
import com.example.PropertyDemo.Repositories.AgentRepository;
import com.example.PropertyDemo.Repositories.PropertyBaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PropertyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertyBaseRepository<Property> propertyRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ServletContext context;


    RentalProperty FIRST_PROPERTY;
    SaleProperty SECOND_PROPERTY;

    Agent FIRST_AGENT;
    Agent SECOND_AGENT;

    String AMAZON_S3_BUCKET_URL = "https://propertytestbucket.s3.eu-west-2.amazonaws.com/";
    String AMAZON_S3_BUCKET_URL_REGEX = "https://propertytestbucket\\.s3\\.eu-west-2\\.amazonaws\\.com/";
    String UUID_REGEX = "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$";

    @BeforeEach
    private void generateObjects() throws MalformedURLException {
        FIRST_AGENT = new Agent("Test Agent", new Location(1, "Test Agent Street", "Test Agent City",
                "Test Agent County", "Test Agent Postcode"), "Test Agent Telephone",
                new URL("http://testAgentLogo"));

        FIRST_AGENT = agentRepository.save(FIRST_AGENT);

        SECOND_AGENT = new Agent("Test Agent Name 2", new Location(2, "Test Agent Street 2",
                "Test Agent City 2", "Test Agent County 2", "Test Agent Postcode 2"),
                "Test Agent Telephone 2", new URL("http://testurl2"));

        SECOND_AGENT = agentRepository.save(SECOND_AGENT);

        FIRST_PROPERTY = new RentalProperty(PropertyType.HOUSE_TERRACED,
                new Location(1, "Test Street", "Test City", "Test County", "Test Postcode" ),
                6, Arrays.asList(new URL("http://propertyImage1url"),
                new URL("http://propertyImage2url"),
                new URL("http://propertyImage3url")),
                FIRST_AGENT,
                2400);

        FIRST_PROPERTY = propertyRepository.save(FIRST_PROPERTY);

        SECOND_PROPERTY = new SaleProperty(PropertyType.FLAT,
                new Location(2, "Test Street 2", "Test City 2", "Test County 2",
                        "Test Postcode 2"), 2, Arrays.asList(
                                new URL("http://property2Image1url"),
                new URL("http://property2Image2url"), new URL("http://property2Image3url")),
                FIRST_AGENT, 1200);

        SECOND_PROPERTY = propertyRepository.save(SECOND_PROPERTY);

        FIRST_AGENT.addProperty(FIRST_PROPERTY);
        FIRST_AGENT.addProperty(SECOND_PROPERTY);

        FIRST_AGENT = agentRepository.save(FIRST_AGENT);

    }



    @Test
    public void getAllProperties() throws Exception {
        mockMvc.perform(get("/properties").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath( "[0].id", is(FIRST_PROPERTY.getId())))
                .andExpect(jsonPath( "[0].type", is(FIRST_PROPERTY.getType().name())))
                .andExpect(jsonPath(  "[0].location.number", is(FIRST_PROPERTY.getLocation().getNumber())))
                .andExpect(jsonPath(  "[0].location.street", is (FIRST_PROPERTY.getLocation().getStreet())))
                .andExpect(jsonPath(  "[0].location.city", is (FIRST_PROPERTY.getLocation().getCity())))
                .andExpect(jsonPath(  "[0].location.county", is(FIRST_PROPERTY.getLocation().getCounty())))
                .andExpect(jsonPath(  "[0].location.postCode", is(FIRST_PROPERTY.getLocation().getPostCode())))
                .andExpect(jsonPath(  "[0].bedrooms", is(FIRST_PROPERTY.getBedrooms())))
                .andExpect(jsonPath(  "[0].images[0]", is(FIRST_PROPERTY.getImages().get(0).toExternalForm())))
                .andExpect(jsonPath(  "[0].images[1]", is(FIRST_PROPERTY.getImages().get(1).toExternalForm())))
                .andExpect(jsonPath(  "[0].images[2]", is(FIRST_PROPERTY.getImages().get(2).toExternalForm())))
                .andExpect(jsonPath("$", hasNoJsonPath("[0].agent")))
                .andExpect(jsonPath(  "[1].id", is(SECOND_PROPERTY.getId())))
                .andExpect(jsonPath(  "[1].type", is(SECOND_PROPERTY.getType().name())))
                .andExpect(jsonPath(  "[1].location.number", is(SECOND_PROPERTY.getLocation().getNumber())))
                .andExpect(jsonPath(  "[1].location.street", is(SECOND_PROPERTY.getLocation().getStreet())))
                .andExpect(jsonPath(  "[1].location.city", is(SECOND_PROPERTY.getLocation().getCity())))
                .andExpect(jsonPath(  "[1].location.county", is(SECOND_PROPERTY.getLocation().getCounty())))
                .andExpect(jsonPath(  "[1].location.postCode", is(SECOND_PROPERTY.getLocation().getPostCode())))
                .andExpect(jsonPath(  "[1].bedrooms", is(SECOND_PROPERTY.getBedrooms())))
                .andExpect(jsonPath(  "[1].images[0]", is(SECOND_PROPERTY.getImages().get(0).toExternalForm())))
                .andExpect(jsonPath(  "[1].images[1]", is(SECOND_PROPERTY.getImages().get(1).toExternalForm())))
                .andExpect(jsonPath(  "[1].images[2]", is(SECOND_PROPERTY.getImages().get(2).toExternalForm())))
                .andExpect(jsonPath("$", hasNoJsonPath("[0].agent")));

    }

    @Test
    void getOnlyRentals() throws Exception {
        mockMvc.perform(get("/rentalProperties").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$[0].id", is(FIRST_PROPERTY.getId())))
                .andExpect(jsonPath("$[0].type", is(FIRST_PROPERTY.getType().name())))
                .andExpect(jsonPath("$[0].location.number", is(FIRST_PROPERTY.getLocation().getNumber())))
                .andExpect(jsonPath("$[0].location.street", is(FIRST_PROPERTY.getLocation().getStreet())))
                .andExpect(jsonPath("$[0].location.city", is(FIRST_PROPERTY.getLocation().getCity())))
                .andExpect(jsonPath("$[0].location.county", is(FIRST_PROPERTY.getLocation().getCounty())))
                .andExpect(jsonPath("$[0].location.postCode", is(FIRST_PROPERTY.getLocation().getPostCode())))
                .andExpect(jsonPath("$[0].bedrooms", is(FIRST_PROPERTY.getBedrooms())))
                .andExpect(jsonPath("$[0].images[0]", is(FIRST_PROPERTY.getImages().get(0).toExternalForm())))
                .andExpect(jsonPath("$[0].images[1]", is(FIRST_PROPERTY.getImages().get(1).toExternalForm())))
                .andExpect(jsonPath("$[0].images[2]", is(FIRST_PROPERTY.getImages().get(2).toExternalForm())))
                .andExpect(jsonPath("$[0].monthlyRent", is(FIRST_PROPERTY.getMonthlyRent())));
    }

    @Test
    void getOnlySales() throws Exception {
        mockMvc.perform(get("/salesProperties").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$[0].id", is(SECOND_PROPERTY.getId())))
                .andExpect(jsonPath("$[0].type", is(SECOND_PROPERTY.getType().name())))
                .andExpect(jsonPath("$[0].location.number", is(SECOND_PROPERTY.getLocation().getNumber())))
                .andExpect(jsonPath("$[0].location.street", is(SECOND_PROPERTY.getLocation().getStreet())))
                .andExpect(jsonPath("$[0].location.city", is(SECOND_PROPERTY.getLocation().getCity())))
                .andExpect(jsonPath("$[0].location.county", is(SECOND_PROPERTY.getLocation().getCounty())))
                .andExpect(jsonPath("$[0].location.postCode", is(SECOND_PROPERTY.getLocation().getPostCode())))
                .andExpect(jsonPath("$[0].bedrooms", is(SECOND_PROPERTY.getBedrooms())))
                .andExpect(jsonPath("$[0].images[0]", is(SECOND_PROPERTY.getImages().get(0).toExternalForm())))
                .andExpect(jsonPath("$[0].images[1]", is(SECOND_PROPERTY.getImages().get(1).toExternalForm())))
                .andExpect(jsonPath("$[0].images[2]", is(SECOND_PROPERTY.getImages().get(2).toExternalForm())))
                .andExpect(jsonPath("$[0].price", is(SECOND_PROPERTY.getPrice())));

    }

    @Test
    public void getAllAgents() throws Exception {
        mockMvc.perform(get("/agents").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$[0].id", is(FIRST_AGENT.getId())))
                .andExpect(jsonPath("$[0].name", is(FIRST_AGENT.getName())))
                .andExpect(jsonPath("$[0].location.number",
                        is(FIRST_AGENT.getLocation().getNumber())))
                .andExpect(jsonPath("$[0].location.street",
                        is(FIRST_AGENT.getLocation().getStreet())))
                .andExpect(jsonPath("$[0].location.city",
                        is(FIRST_AGENT.getLocation().getCity())))
                .andExpect(jsonPath("$[0].location.county",
                        is(FIRST_AGENT.getLocation().getCounty())))
                .andExpect(jsonPath("$[0].location.postCode",
                        is(FIRST_AGENT.getLocation().getPostCode())))
                .andExpect(jsonPath("$[0].telephoneNumber",
                        is(FIRST_AGENT.getTelephoneNumber())))
                .andExpect(jsonPath("$[0].logoImage",
                        is(FIRST_AGENT.getLogoImage().toExternalForm())))
                .andExpect(jsonPath("$[1].id", is(SECOND_AGENT.getId())))
                .andExpect(jsonPath("$[1].name", is(SECOND_AGENT.getName())))
                .andExpect(jsonPath("$[1].location.number",
                        is(SECOND_AGENT.getLocation().getNumber())))
                .andExpect(jsonPath("$[1].location.street",
                        is(SECOND_AGENT.getLocation().getStreet())))
                .andExpect(jsonPath("$[1].location.city",
                        is(SECOND_AGENT.getLocation().getCity())))
                .andExpect(jsonPath("$[1].location.postCode",
                        is(SECOND_AGENT.getLocation().getPostCode())))
                .andExpect(jsonPath("$[1].telephoneNumber",
                        is(SECOND_AGENT.getTelephoneNumber())))
                .andExpect(jsonPath("$[1].logoImage",
                        is(SECOND_AGENT.getLogoImage().toExternalForm())));
    }

    @Test
    public void getAgentForProperty() throws Exception {
        mockMvc.perform(get("/properties/" + FIRST_PROPERTY.getId() + "/agent").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$.id", is(FIRST_AGENT.getId())))
                .andExpect(jsonPath("$.name", is(FIRST_AGENT.getName())))
                .andExpect(jsonPath("$.location.number", is(FIRST_AGENT.getLocation().getNumber())))
                .andExpect(jsonPath("$.location.city", is(FIRST_AGENT.getLocation().getCity())))
                .andExpect(jsonPath("$.location.county", is(FIRST_AGENT.getLocation().getCounty())))
                .andExpect(jsonPath("$.location.postCode", is(FIRST_AGENT.getLocation().getPostCode())))
                .andExpect(jsonPath("$.telephoneNumber", is(FIRST_AGENT.getTelephoneNumber())))
                .andExpect(jsonPath("$.logoImage", is(FIRST_AGENT.getLogoImage().toExternalForm())));
    }



    @Test
    public void getAgentProperties() throws Exception {
        mockMvc.perform(get("/agents/" + FIRST_AGENT.getId() + "/properties"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$[0].id", is(FIRST_PROPERTY.getId())))
                .andExpect(jsonPath("$[0].type", is(FIRST_PROPERTY.getType().name())))
                .andExpect(jsonPath("$[0].location.number", is(FIRST_PROPERTY.getLocation().getNumber())))
                .andExpect(jsonPath("$[0].location.street", is(FIRST_PROPERTY.getLocation().getStreet())))
                .andExpect(jsonPath("$[0].location.city", is(FIRST_PROPERTY.getLocation().getCity())))
                .andExpect(jsonPath("$[0].location.county", is(FIRST_PROPERTY.getLocation().getCounty())))
                .andExpect(jsonPath("$[0].location.postCode", is(FIRST_PROPERTY.getLocation().getPostCode())))
                .andExpect(jsonPath("$[0].bedrooms", is(FIRST_PROPERTY.getBedrooms())))
                .andExpect(jsonPath("$[0].images[0]", is(FIRST_PROPERTY.getImages().get(0).toExternalForm())))
                .andExpect(jsonPath("$[0].images[1]", is(FIRST_PROPERTY.getImages().get(1).toExternalForm())))
                .andExpect(jsonPath("$[0].images[2]", is(FIRST_PROPERTY.getImages().get(2).toExternalForm())))
                .andExpect(jsonPath("$[0].monthlyRent", is(FIRST_PROPERTY.getMonthlyRent())))
                .andExpect(jsonPath("$[1].id", is(SECOND_PROPERTY.getId())))
                .andExpect(jsonPath("$[1].type", is(SECOND_PROPERTY.getType().name())))
                .andExpect(jsonPath("$[1].location.number", is(SECOND_PROPERTY.getLocation().getNumber())))
                .andExpect(jsonPath("$[1].location.street", is(SECOND_PROPERTY.getLocation().getStreet())))
                .andExpect(jsonPath("$[1].location.city", is(SECOND_PROPERTY.getLocation().getCity())))
                .andExpect(jsonPath("$[1].location.county", is(SECOND_PROPERTY.getLocation().getCounty())))
                .andExpect(jsonPath("$[1].location.postCode", is(SECOND_PROPERTY.getLocation().getPostCode())))
                .andExpect(jsonPath("$[1].bedrooms", is(SECOND_PROPERTY.getBedrooms())))
                .andExpect(jsonPath("$[1].images[0]", is(SECOND_PROPERTY.getImages().get(0).toExternalForm())))
                .andExpect(jsonPath("$[1].images[1]", is(SECOND_PROPERTY.getImages().get(1).toExternalForm())))
                .andExpect(jsonPath("$[1].images[2]", is(SECOND_PROPERTY.getImages().get(2).toExternalForm())))
                .andExpect(jsonPath("$[1].price", is(SECOND_PROPERTY.getPrice())));
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    public void addRentalProperty() throws Exception {

        RentalProperty newProperty = new RentalProperty(PropertyType.FLAT,
                new Location(1, "Test Street", "Test City", "Test County",
                        "Test PostCode"),
                3, new ArrayList<>(), null, 1500);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(newProperty);
        MockMultipartFile jsonMultiPart = new MockMultipartFile("property", "newProperty",
                "application/json", json.getBytes());

        List<MockMultipartFile> files = createImageMultipart(3);

        mockMvc.perform(multipart("/agents/" + FIRST_AGENT.getId() + "/properties/rentals")
                .file(jsonMultiPart).file(files.get(0)).file(files.get(1)).file(files.get(2)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.location.number", is(newProperty.getLocation().getNumber())))
                .andExpect(jsonPath("$.location.street", is(newProperty.getLocation().getStreet())))
                .andExpect(jsonPath("$.location.city", is(newProperty.getLocation().getCity())))
                .andExpect(jsonPath("$.location.county", is(newProperty.getLocation().getCounty())))
                .andExpect(jsonPath("$.location.postCode", is(newProperty.getLocation().getPostCode())))
                .andExpect(jsonPath("$.bedrooms", is(newProperty.getBedrooms())))
                .andExpect(jsonPath("$.images[0]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_0")))
                .andExpect(jsonPath("$.images[1]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_1")))
                .andExpect(jsonPath("$.images[2]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_2")))
                .andExpect(jsonPath("$", hasNoJsonPath("$.agent")))
                .andExpect(jsonPath("$.monthlyRent", is((double) newProperty.getMonthlyRent())));

    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    public void addSaleProperty() throws Exception {

        SaleProperty newProperty = new SaleProperty(PropertyType.HOUSE_TERRACED,
                new Location(1, "Test Street", "Test City", "Test County", "Test PostCode"),
                3, new ArrayList<>(), null, 700000);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(newProperty);
        MockMultipartFile jsonMultiPart = new MockMultipartFile("property", "newProperty",
                "application/json", json.getBytes());

        List<MockMultipartFile> files = createImageMultipart(3);

        mockMvc.perform(multipart("/agents/" + FIRST_AGENT.getId() + "/properties/sales")
                .file(jsonMultiPart).file(files.get(0)).file(files.get(1)).file(files.get(2)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue()))).andExpect(jsonPath("$.location.number", is(newProperty.getLocation().getNumber())))
                .andExpect(jsonPath("$.location.street", is(newProperty.getLocation().getStreet())))
                .andExpect(jsonPath("$.location.city", is(newProperty.getLocation().getCity())))
                .andExpect(jsonPath("$.location.county", is(newProperty.getLocation().getCounty())))
                .andExpect(jsonPath("$.location.postCode", is(newProperty.getLocation().getPostCode())))
                .andExpect(jsonPath("$.bedrooms", is(newProperty.getBedrooms())))
                .andExpect(jsonPath("$.images[0]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_0")))
                .andExpect(jsonPath("$.images[1]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_1")))
                .andExpect(jsonPath("$.images[2]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_2")))
                .andExpect(jsonPath("$", hasNoJsonPath("$.agent")))
                .andExpect(jsonPath("$.price", is((double) newProperty.getPrice())));

    }



    private List<MockMultipartFile> createImageMultipart(int size) {
        List<MockMultipartFile> files = new ArrayList<>();
        IntStream.range(0, size).forEach(i -> {
            String filename = "test_image_" + (i + 1) + ".jpeg";
            try {
                Path path = new ClassPathResource(filename).getFile().toPath();
                files.add(new MockMultipartFile("images", filename, "image/jpg",
                        Files.readAllBytes(path)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return files;
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    public void addImagesToProperty() throws Exception {
        List<MockMultipartFile> files = createImageMultipart(3);
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/properties/3/images");
        builder.with(request -> { request.setMethod("PATCH"); return request;});

        mockMvc.perform(builder
                .file(files.get(0)).file(files.get(1)).file(files.get(2)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[3]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_3")))
                .andExpect(jsonPath("$.images[4]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_4")))
                .andExpect(jsonPath("$.images[5]", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_property_image_5")));
    }



    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void addAgent() throws Exception {
        Agent agent = new Agent("Agent name", new Location(1, "Test Street", "Test City",
                "Test County", "Test PostCode"), "Test Telephone Number");

        String json = new ObjectMapper().writeValueAsString(agent);
        MockMultipartFile jsonFile = new MockMultipartFile("agent", "agent",
                "application/json", json.getBytes());

        Path path = new ClassPathResource("test_image_1.jpeg").getFile().toPath();
        MockMultipartFile file = new MockMultipartFile("logo", "test_image_1.jpeg",
                "image/jpg", Files.readAllBytes(path));

        mockMvc.perform(multipart("/agents").file(file).file(jsonFile).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is(agent.getName())))
                .andExpect(jsonPath("$.location.number", is(agent.getLocation().getNumber())))
                .andExpect(jsonPath("$.location.street", is(agent.getLocation().getStreet())))
                .andExpect(jsonPath("$.location.city", is(agent.getLocation().getCity())))
                .andExpect(jsonPath("$.location.county", is(agent.getLocation().getCounty())))
                .andExpect(jsonPath("$.location.postCode", is(agent.getLocation().getPostCode())))
                .andExpect(jsonPath("$.telephoneNumber", is(agent.getTelephoneNumber())))
                .andExpect(jsonPath("$.logoImage", matchesPattern(AMAZON_S3_BUCKET_URL_REGEX + "\\d_logo")));
    }

    @Test
    void authorisationRequiredToAddAgent() throws Exception {
        mockMvc.perform(multipart("/agents").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorisationRequiredToAddSaleProperty() throws Exception {
        mockMvc.perform(multipart("/agents/1/properties/rentals").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorisationRequiredToAddRentalProperty() throws Exception {
        mockMvc.perform(multipart("/agents/1/properties/sale").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorisationRequiredToAddImagesToProperty() throws Exception {
        mockMvc.perform(multipart("/property/1/images").with(csrf()))
                .andExpect(status().isForbidden());
    }







}
