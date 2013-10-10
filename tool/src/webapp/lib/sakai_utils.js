var SakaiUtils;

(function()
{
	if(SakaiUtils == null)
		SakaiUtils = {};

    // CLOG-27 and PRFL-566
    var jqueryImport = /<script type="text\/javascript" src="\/profile2-tool\/javascript\/jquery-[\w\.]*\.js">\s*<\/script>/;
		
	SakaiUtils.getProfileMarkup = function(userId) {
		var profile = '';

		jQuery.ajax( {
	       	url : "/direct/profile/" + userId + "/formatted",
	       	dataType : "html",
	       	async : false,
			cache: false,
		   	success : function(p) {

                // CLOG-27 and PRFL-566
                if(p.match(jqueryImport)) {
                    p = p.replace(jqueryImport,'');
                }

				profile = p;
			},
			error : function(xmlHttpRequest,stat,error) {
				//alert("Failed to get profile markup. Status: " + stat + ". Error: " + error);
			}
	   	});

		return profile;
	}
		
	SakaiUtils.renderTrimpathTemplate = function(templateName,contextObject,output) {
		var templateNode = document.getElementById(templateName);
		var firstNode = templateNode.firstChild;
		var template = null;

		if ( firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4))
  			template = templateNode.firstChild.data.toString();
		else
   			template = templateNode.innerHTML.toString();

		var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

   		var render = trimpathTemplate.process(contextObject);

		if (output)
			document.getElementById(output).innerHTML = render;

		return render;
	}

	SakaiUtils.setupFCKEditor = function(textarea_id,width,height) {

        sakai.editor.launch(textarea_id,{},width,height);
        
        try {
            if(window.frameElement) {
                setMainFrameHeight(window.frameElement.id);
            }
        } catch (e) {
            return;
        }
	}
	
	SakaiUtils.setupCKEditor = function(textarea_id,width,height) {

        if (CKEDITOR.instances[textarea_id]) {
            CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
        }

        sakai.editor.launch(textarea_id,{},width,height);
        
        CKEDITOR.instances[textarea_id].on('instanceReady',function (e) {
            try {
                if(window.frameElement) {
                    setMainFrameHeight(window.frameElement.id);
                }
            } catch (e) {
                return;
            }
        });
	}
	
	SakaiUtils.setupWysiwygEditor = function(editorId,textarea_id,width,height) {
		if ('FCKeditor' === editorId) {
			SakaiUtils.setupFCKEditor(textarea_id,width,height);
		} else if ('ckeditor' === editorId) {
			SakaiUtils.setupCKEditor(textarea_id,width,height);
		}
	}
	
	SakaiUtils.getWysiwygEditor = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return FCKeditorAPI.GetInstance(textarea_id);
		} else if ('ckeditor' === editorId) {
			return CKEDITOR.instances[textarea_id];
		}
	}
	
	SakaiUtils.getEditorData = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return SakaiUtils.getWysiwygEditor(editorId,textarea_id).GetXHTML(true);
		} else if ('ckeditor' === editorId) {
			return SakaiUtils.getWysiwygEditor(editorId,textarea_id).getData();
		} else {
            return $('#' + textarea_id).val();
        }
	}
	
	SakaiUtils.resetEditor = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			SakaiUtils.getWysiwygEditor(editorId,textarea_id).ResetIsDirty();
		} else if ('ckeditor' === editorId) {
			SakaiUtils.getWysiwygEditor(editorId,textarea_id).resetDirty();
		} else {
			clogTextAreaChanged = false;
		}
	}
		
	SakaiUtils.isEditorDirty = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return SakaiUtils.getWysiwygEditor(editorId,textarea_id).IsDirty();
		} else if ('ckeditor' === editorId) {
			return SakaiUtils.getWysiwygEditor(editorId,textarea_id).checkDirty();
		} else {
			return clogTextAreaChanged;
		}
	}

}) ();
