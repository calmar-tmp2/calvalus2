package com.bc.calvalus.portal.client;

import com.bc.calvalus.portal.shared.BackendService;
import com.bc.calvalus.portal.shared.BackendServiceAsync;
import com.bc.calvalus.portal.shared.PortalProductionParameter;
import com.bc.calvalus.portal.shared.PortalProductionRequest;
import com.bc.calvalus.portal.shared.PortalProductionResponse;
import com.bc.calvalus.portal.shared.PortalProductionState;
import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public class BackendServiceTest extends GWTTestCase {

    /**
     * Must refer to a valid module that sources this class.
     */
    public String getModuleName() {
        return "com.bc.calvalus.portal.CalvalusPortalJUnit";
    }

    /**
     * Tests the ProcessingRequestVerifier.
     */
    public void testFieldVerifier() {
        assertFalse(PortalProductionRequest.isValid(new PortalProductionRequest(null)));
        assertFalse(PortalProductionRequest.isValid(new PortalProductionRequest("")));
        assertTrue(PortalProductionRequest.isValid(new PortalProductionRequest("ice cream")));
    }

    /**
     * This test will send a request to the server using the 'orderProduction' method in
     * BackendService and verify the response.
     */
    public void testOrderProduction() {
        // Create the service that we will test.
        BackendServiceAsync backendService = createBackendService();

        // Since RPC calls are asynchronous, we will need to wait for a response
        // after this test method returns. This line tells the test runner to wait
        // up to 10 seconds before timing out.
        delayTestFinish(10000);

        // Send a request to the server.
        backendService.orderProduction(new PortalProductionRequest("x*y",
                                                                   new PortalProductionParameter("x", "3"),
                                                                   new PortalProductionParameter("y", "-1")),
                                       new PortalProductionResponseAsyncCallback());
    }

    private BackendServiceAsync createBackendService() {
        BackendServiceAsync backendService = GWT.create(BackendService.class);
        ServiceDefTarget target = (ServiceDefTarget) backendService;
        target.setServiceEntryPoint(GWT.getModuleBaseURL() + "calvalus/backend");
        return backendService;
    }


    private class PortalProductionResponseAsyncCallback implements AsyncCallback<PortalProductionResponse> {
        public void onSuccess(PortalProductionResponse response) {
            // Verify that the response is correct.
            assertNotNull(response);
            assertNotNull(response.getProduction());
            assertNotNull(response.getProduction().getId());
            assertNotNull(response.getProduction().getName());
            assertNotNull(response.getProduction().getStatus());
            assertEquals(PortalProductionState.WAITING, response.getProduction().getStatus().getState());

            // Now that we have received a response, we need to tell the test runner
            // that the test is complete. You must call finishTest() after an
            // asynchronous test finishes successfully, or the test will time out.
            finishTest();
        }

        public void onFailure(Throwable caught) {
            // The request resulted in an unexpected error.
            fail("Request failure: " + caught.getMessage());
        }

    }
}
