clog.sakai = {
    // CLOG-27 and PRFL-566
    jqueryImport: /<script type="text\/javascript" src="\/profile2-tool\/javascript\/jquery-[\w\.]*\.js">\s*<\/script>/,
	getProfileMarkup: function (userId) {

		var profile = '';

		jQuery.ajax( {
	       	url: "/direct/profile/" + userId + "/formatted",
	       	dataType: "html",
	       	async: false,
			cache: false,
		   	success: function (p) {

                // CLOG-27 and PRFL-566
                if(p.match(this.jqueryImport)) {
                    p = p.replace(this.jqueryImport, '');
                }

				profile = p;
			},
			error : function (xmlHttpRequest, textStatus, error) {
				//alert("Failed to get profile markup. Status: " + textStatus + ". Error: " + error);
			}
	   	});

		return profile;
	},
    setupFCKEditor: function (textarea_id, width, height) {

        sakai.editor.launch(textarea_id, {}, width, height);
        
        clog.fitFrame();
	},
    setupCKEditor: function (textarea_id, width, height) {

        if (CKEDITOR.instances[textarea_id]) {
            CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
        }

        sakai.editor.launch(textarea_id,{},width,height);
        
        CKEDITOR.instances[textarea_id].on('instanceReady',function (e) {
            clog.fitFrame();
        });
	},
    setupWysiwygEditor: function (editorId, textarea_id, width, height) {

		if ('FCKeditor' === editorId) {
			this.setupFCKEditor(textarea_id,width,height);
		} else if ('ckeditor' === editorId) {
			this.setupCKEditor(textarea_id,width,height);
		}
	},
    getWysiwygEditor: function (editorId, textarea_id) {

		if ('FCKeditor' === editorId) {
			return FCKeditorAPI.GetInstance(textarea_id);
		} else if ('ckeditor' === editorId) {
			return CKEDITOR.instances[textarea_id];
		}
	},
    getEditorData: function (editorId, textarea_id) {

		if ('FCKeditor' === editorId) {
			return this.getWysiwygEditor(editorId, textarea_id).GetXHTML(true);
		} else if ('ckeditor' === editorId) {
			return this.getWysiwygEditor(editorId, textarea_id).getData();
		}
	},
    resetEditor: function (editorId, textarea_id) {

		if ('FCKeditor' === editorId) {
			this.getWysiwygEditor(editorId, textarea_id).ResetIsDirty();
		} else if ('ckeditor' === editorId) {
			this.getWysiwygEditor(editorId, textarea_id).resetDirty();
		}
	},
    isEditorDirty: function (editorId, textarea_id) {

		if ('FCKeditor' === editorId) {
			return this.getWysiwygEditor(editorId, textarea_id).IsDirty();
		} else if ('ckeditor' === editorId) {
			return this.getWysiwygEditor(editorId, textarea_id).checkDirty();
		}
	}
};
