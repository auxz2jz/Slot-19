package com.auxz2jz.videogrammetry

/** Stable, NON-OVERLAPPING owned output paths. Never alias two algorithms. */
object CloudArtifacts {
    const val SCENE_PLY="sparse_two_view.ply"
    const val ROI_RECONSTRUCTED_PLY="sparse_object_reconstructed.ply"
    const val FILTERED_SCENE_PLY="sparse_object_focus.ply"
    const val MULTIVIEW_PLY="sparse_object_multiview.ply"
    const val SILHOUETTE_HULL_PLY="silhouette_voxel_hull.ply"
    const val MASK_FUSION_PLY="mask_verified_sparse.ply"
    const val SILHOUETTE_REPORT="silhouette_hull_report.json"
    const val ROI_RECONSTRUCTED_REPORT="early_object_reconstruction_report.json"
    const val FILTERED_SCENE_REPORT="object_focus_report.json"
    const val MULTIVIEW_REPORT="object_multiview_report.json"
    fun distinct():Boolean= setOf(SCENE_PLY,ROI_RECONSTRUCTED_PLY,
        FILTERED_SCENE_PLY,MULTIVIEW_PLY,
        SILHOUETTE_HULL_PLY,MASK_FUSION_PLY).size==6 &&
        ROI_RECONSTRUCTED_REPORT!=FILTERED_SCENE_REPORT &&
        MULTIVIEW_REPORT!=ROI_RECONSTRUCTED_REPORT
}
