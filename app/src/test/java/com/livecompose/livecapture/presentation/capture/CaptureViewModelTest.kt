package com.livecompose.livecapture.presentation.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureViewModelPipelineStageTest {

    @Test
    fun `PipelineStage has 9 stages`() {
        assertEquals(9, CaptureViewModel.PipelineStage.values().size)
    }

    @Test
    fun `PipelineStage contains all expected stages`() {
        val stages = CaptureViewModel.PipelineStage.values().toList()
        assertEquals(CaptureViewModel.PipelineStage.IDLE, stages[0])
        assertEquals(CaptureViewModel.PipelineStage.STARTING_CAMERA, stages[1])
        assertEquals(CaptureViewModel.PipelineStage.WAITING_FOR_STABILITY, stages[2])
        assertEquals(CaptureViewModel.PipelineStage.DETECTING_REGION, stages[3])
        assertEquals(CaptureViewModel.PipelineStage.TEMPLATE_READY, stages[4])
        assertEquals(CaptureViewModel.PipelineStage.READY_TO_CAPTURE, stages[5])
        assertEquals(CaptureViewModel.PipelineStage.CAPTURING_PHOTO, stages[6])
        assertEquals(CaptureViewModel.PipelineStage.SAVING_PHOTO, stages[7])
        assertEquals(CaptureViewModel.PipelineStage.ERROR, stages[8])
    }
}
