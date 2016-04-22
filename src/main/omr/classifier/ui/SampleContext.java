//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S a m p l e C o n t e x t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.SampleSheet;

import omr.run.RunTable;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;
import static omr.ui.selection.MouseMovement.PRESSING;
import static omr.ui.selection.SelectionHint.LOCATION_INIT;
import omr.ui.selection.SelectionService;
import omr.ui.selection.UserEvent;
import omr.ui.view.Rubber;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;
import omr.ui.view.ZoomAssembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code SampleContext} displays a sample within the context image of its
 * containing sheet.
 *
 * @author Hervé Bitteur
 */
public class SampleContext
        extends ZoomAssembly
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleContext.class);

    private static final Dimension NO_DIM = new Dimension(0, 0);

    private static final Point NO_OFFSET = new Point(0, 0);

    //~ Instance fields ----------------------------------------------------------------------------
    private final ContextView contextView;

    private final SelectionService locationService = new SelectionService(
            "sampleLocationService",
            new Class<?>[]{LocationEvent.class});

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleContext} object.
     */
    public SampleContext ()
    {
        contextView = new ContextView(zoom, rubber);
        contextView.setLocationService(locationService);
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // connect //
    //---------//
    public void connect (EntityService<Sample> sampleService)
    {
        sampleService.subscribeStrongly(EntityListEvent.class, contextView);
        locationService.subscribeStrongly(LocationEvent.class, contextView);
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of this component.
     */
    private void defineLayout ()
    {
        component.add(new ScrollView(contextView).getComponent(), BorderLayout.CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ContextView //
    //-------------//
    private class ContextView
            extends RubberPanel
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Current sample, if any. */
        private Sample sample;

        /** RunTable of sheet image, if any. */
        private RunTable sheetTable;

        //~ Constructors ---------------------------------------------------------------------------
        public ContextView (Zoom zoom,
                            Rubber rubber)
        {
            super(zoom, rubber);
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // onEvent //
        //---------//
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof LocationEvent) {
                    // Location => move view focus on this location w/ markers
                    LocationEvent locationEvent = (LocationEvent) event;
                    showFocusLocation(locationEvent.getData(), true);
                } else if (event instanceof EntityListEvent) {
                    // Sample => sample, sheet & location
                    handleEvent((EntityListEvent<Sample>) event);
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }

        @Override
        protected void render (Graphics2D g)
        {
            if (sheetTable != null) {
                g.setColor(Color.LIGHT_GRAY);
                sheetTable.render(g, new Point(0, 0));
            }
        }

        @Override
        protected void renderItems (Graphics2D g)
        {
            if (sample != null) {
                g.setColor(Color.BLUE);
                sample.getRunTable()
                        .render(g, (sheetTable != null) ? sample.getTopLeft() : NO_OFFSET);
            }
        }

        //-------------//
        // handleEvent //
        //-------------//
        private void handleEvent (EntityListEvent<Sample> sampleListEvent)
        {
            Dimension dim = NO_DIM;
            Rectangle rect = null;
            sample = sampleListEvent.getEntity();

            if (sample != null) {
                logger.debug("SampleContext sample:{}", sample);

                SampleRepository repository = SampleRepository.getInstance();
                SampleSheet sampleSheet = repository.getSampleSheet(sample);
                sheetTable = (sampleSheet != null) ? sampleSheet.getImage() : null;

                if (sheetTable != null) {
                    dim = sheetTable.getDimension();
                    rect = sample.getBounds();
                }
            }

            setModelSize(dim);
            locationService.publish(new LocationEvent(this, LOCATION_INIT, PRESSING, rect));
            repaint();
        }
    }
}