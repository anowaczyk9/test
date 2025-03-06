package pl.santander.clp.product.common.relations.snapshot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RelationDetailsServiceTest {

    private static final String TEST_FILE = "relation_test_cases.json";

    static Stream<TestCase> testCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = RelationDetailsServiceTest.class.getClassLoader().getResourceAsStream(TEST_FILE);
        List<TestCase> testCases = mapper.readValue(is, new TypeReference<List<TestCase>>() {});
        return testCases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void testRelationDetails(TestCase testCase) {
        // Arrange
        Set<ProductRelationSnapshot> relations = new HashSet<>(testCase.getRelations());
        RelationDetailsService service = new RelationDetailsService(testCase.getProductId(), relations);
        
        // Act & Assert based on relation type and role
        switch (testCase.getRelationType()) {
            case "MODIFICATION":
                testModificationRelation(service, testCase);
                break;
            case "ACTIVATION":
                testActivationRelation(service, testCase);
                break;
            case "RENEWAL":
                testRenewalRelation(service, testCase);
                break;
            case "RESIGNATION":
                testResignationRelation(service, testCase);
                break;
            case "REFINANCING":
                testRefinancingRelation(service, testCase);
                break;
            case "ABSORPTION":
                testAbsorptionRelation(service, testCase);
                break;
            case "MULTIPLE":
                testMultipleRelations(service);
                break;
            case "NONE":
                testNoRelations(service);
                break;
        }
    }
    
    private void testModificationRelation(RelationDetailsService service, TestCase testCase) {
        if ("TARGET".equals(testCase.getRole())) {
            assertTrue(service.isModification());
            assertFalse(service.isModificationSourceProduct());
            assertEquals(testCase.getOtherProductId(), service.getModifiedProductId());
        } else { // SOURCE
            assertFalse(service.isModification());
            assertTrue(service.isModificationSourceProduct());
            assertNull(service.getModifiedProductId());
        }
    }
    
    private void testActivationRelation(RelationDetailsService service, TestCase testCase) {
        if ("TARGET".equals(testCase.getRole())) {
            assertTrue(service.isActivation());
            assertFalse(service.isActivationSourceProduct());
            assertEquals(testCase.getOtherProductId(), service.getActivatedProductId());
        } else { // SOURCE
            assertFalse(service.isActivation());
            assertTrue(service.isActivationSourceProduct());
            assertNull(service.getActivatedProductId());
        }
    }
    
    private void testRenewalRelation(RelationDetailsService service, TestCase testCase) {
        if ("TARGET".equals(testCase.getRole())) {
            assertTrue(service.isRenewal());
            assertFalse(service.isRenewalSourceProduct());
            assertEquals(testCase.getOtherProductId(), service.getRenewedProductId());
        } else { // SOURCE
            assertFalse(service.isRenewal());
            assertTrue(service.isRenewalSourceProduct());
            assertNull(service.getRenewedProductId());
        }
    }
    
    private void testResignationRelation(RelationDetailsService service, TestCase testCase) {
        if ("TARGET".equals(testCase.getRole())) {
            assertTrue(service.isResignation());
            assertFalse(service.isResignationSourceProduct());
            assertEquals(testCase.getOtherProductId(), service.getResignedProductId());
        } else { // SOURCE
            assertFalse(service.isResignation());
            assertTrue(service.isResignationSourceProduct());
            assertNull(service.getResignedProductId());
        }
    }
    
    private void testRefinancingRelation(RelationDetailsService service, TestCase testCase) {
        if ("TARGET".equals(testCase.getRole())) {
            assertTrue(service.doesRefinance(testCase.getOtherProductId()));
            assertFalse(service.isRefinancedBy(testCase.getOtherProductId()));
        } else { // SOURCE
            assertFalse(service.doesRefinance(testCase.getOtherProductId()));
            assertTrue(service.isRefinancedBy(testCase.getOtherProductId()));
        }
    }
    
    private void testAbsorptionRelation(RelationDetailsService service, TestCase testCase) {
        if ("TARGET".equals(testCase.getRole())) {
            assertTrue(service.doesAbsorb(testCase.getOtherProductId()));
            assertFalse(service.isAbsorbedBy(testCase.getOtherProductId()));
        } else { // SOURCE
            assertFalse(service.doesAbsorb(testCase.getOtherProductId()));
            assertTrue(service.isAbsorbedBy(testCase.getOtherProductId()));
        }
    }
    
    private void testMultipleRelations(RelationDetailsService service) {
        // Test for the multiple relations case
        assertTrue(service.isModification());
        assertTrue(service.isActivation());
        assertEquals(1L, service.getModifiedProductId());
        assertEquals(2L, service.getActivatedProductId());
        
        // Confirm other relations are not present
        assertFalse(service.isRenewal());
        assertFalse(service.isResignation());
    }
    
    private void testNoRelations(RelationDetailsService service) {
        // Test that all queries return false or null
        assertFalse(service.isModification());
        assertFalse(service.isModificationSourceProduct());
        assertFalse(service.isActivation());
        assertFalse(service.isActivationSourceProduct());
        assertFalse(service.isRenewal());
        assertFalse(service.isRenewalSourceProduct());
        assertFalse(service.isResignation());
        assertFalse(service.isResignationSourceProduct());
        
        assertNull(service.getModifiedProductId());
        assertNull(service.getActivatedProductId());
        assertNull(service.getRenewedProductId());
        assertNull(service.getResignedProductId());
        
        assertFalse(service.isRefinancedBy(2L));
        assertFalse(service.doesRefinance(2L));
        assertFalse(service.isAbsorbedBy(2L));
        assertFalse(service.doesAbsorb(2L));
    }
    
    // POJO to represent a test case from JSON
    public static class TestCase {
        private String name;
        private Long productId;
        private List<ProductRelationSnapshot> relations;
        private String relationType;
        private String role;
        private Long otherProductId;
        
        // Getters and setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public List<ProductRelationSnapshot> getRelations() {
            return relations;
        }
        
        public void setRelations(List<ProductRelationSnapshot> relations) {
            this.relations = relations;
        }
        
        public String getRelationType() {
            return relationType;
        }
        
        public void setRelationType(String relationType) {
            this.relationType = relationType;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public Long getOtherProductId() {
            return otherProductId;
        }
        
        public void setOtherProductId(Long otherProductId) {
            this.otherProductId = otherProductId;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}