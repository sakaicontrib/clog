clog.sakai = {
    setupWysiwygEditor: function (editorId, textarea_id, width, height) {

        if (CKEDITOR.instances[textarea_id]) {
            CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
        }

        sakai.editor.launch(textarea_id,{},width,height);
	},
    getWysiwygEditor: function (editorId, textarea_id) {
        return CKEDITOR.instances[textarea_id];
	},
    getEditorData: function (editorId, textarea_id) {
        return this.getWysiwygEditor(editorId, textarea_id).getData();
	},
    resetEditor: function (editorId, textarea_id) {
        this.getWysiwygEditor(editorId, textarea_id).resetDirty();
	},
    isEditorDirty: function (editorId, textarea_id) {
        return this.getWysiwygEditor(editorId, textarea_id).checkDirty();
	}
};
