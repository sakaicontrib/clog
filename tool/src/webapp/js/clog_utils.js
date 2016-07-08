clog.utils = {

    joinElementIds: function (jqObj) {

        var ids = '';
        jqObj.each(function (i, el) {

            ids += this.id;
            if (i < (jqObj.length - 1)) ids += ",";
        });
        return ids;
    },
    getCurrentUserPermissions: function (callback) {

        jQuery.ajax( {
            url: "/direct/clog/userPerms.json?siteId=" + clog.siteId,
            dataType: "json",
            cache: false,
            timeout: clog.AJAX_TIMEOUT,
            success: function (json) {
                callback(json.data);
            },
            error : function (xmlHttpRequest, textStatus, error) {
                alert("Failed to get the current user permissions. Status: " + textStatus + ". Error: " + error);
            }
        });
    },
    getSitePermissionMatrix: function (callback) {

        jQuery.ajax( {
            url: "/direct/clog/perms.json?siteId=" + clog.siteId,
            dataType: "json",
            cache: false,
            timeout: clog.AJAX_TIMEOUT,
            success: function(json) {

                var p = json.data;

                var perms = [];

                for (role in p) {
                    var permSet = {'role': role};

                    p[role].forEach(function (p) {
                        eval("permSet." + p.replace(/\./g,"_") + " = true");
                    });

                    perms.push(permSet);
                }

                callback(perms);
            },
            error: function(xmlHttpRequest, textStatus, error) {
                alert("Failed to get permissions. Status: " + textStatus + ". Error: " + error);
            }
        });
    },
    savePermissions: function () {

        var myData = { siteId: clog.siteId };
        $('.clog_permission_checkbox').each(function (b) {

            if (this.checked) {
                myData[this.id] = 'true';
            } else {
                myData[this.id] = 'false';
            }
        });

        jQuery.ajax( {
            url: "/direct/clog/savePerms",
            type: 'POST',
            data: myData,
            dataType: 'text',
            timeout: clog.AJAX_TIMEOUT,
            success: function (result) {
                location.reload();
            },
            error : function(xmlHttpRequest, textStatus, error) {
                alert("Failed to save permissions. Status: " + textStatus + '. Error: ' + error);
            }
        });

        return false;
    },
    attachProfilePopup: function () {

        $('a.profile').cluetip({
            width: '620px',
            cluetipClass: 'clog',
            sticky: true,
            dropShadow: false,
            arrows: true,
            mouseOutClose: true,
            closeText: '<img src="/library/image/silk/cross.png" alt="close" />',
            closePosition: 'top',
            showTitle: false,
            hoverIntent: true
        });
    },
    formatDate: function (millis) {

        if (millis <= 0) {
            return clog.i18n.none;
        } else {
            var d = new Date(millis);
            var hours = d.getHours();
            if (hours < 10) hours = '0' + hours;
            var minutes = d.getMinutes();
            if (minutes < 10) minutes = '0' + minutes;
            return d.getDate() + " " + clog.i18n.months[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
        }
    },
    addFormattedLastPostDatesToGroups: function (groups) {

        groups.forEach(function (group) {
            group.formattedLastPostDate = clog.utils.formatDate(group.lastPostDate);
        });
    },
    addFormattedDatesToPosts: function (posts) {

        posts.forEach(function (p) {
            clog.utils.addFormattedDateToPost(p);
        });
    },
    addFormattedDateToPost: function(post) {

        post.formattedCreatedDate = this.formatDate(post.createdDate);
        post.formattedModifiedDate = this.formatDate(post.modifiedDate);

        post.comments.forEach(function (c) {

            c.formattedCreatedDate = clog.utils.formatDate(c.createdDate);
            c.formattedModifiedDate = clog.utils.formatDate(c.modifiedDate);
        });
    },
    addFormattedDatesToCurrentPost: function () {
        this.addFormattedDateToPost(clog.currentPost);
    },
    autosavePost: function (wysiwygEditor) {

        if (!clog.sakai.isEditorDirty(wysiwygEditor, 'clog_content_editor') && !clog.titleChanged) {
            return 0;
        }
    
        return clog.utils.storePost('AUTOSAVE',null,wysiwygEditor);
    },
    savePostAsDraft: function (wysiwygEditor) {
        return this.storePost('PRIVATE',null,wysiwygEditor);
    },
    publishPost: function (wysiwygEditor) {
        return this.storePost('READY',true,wysiwygEditor);
    },
    publicisePost: function (wysiwygEditor) {

        if (confirm(clog.i18n.public_question)) {
            return this.storePost('PUBLIC',null,wysiwygEditor);
        }
    },
    storePost: function (visibility, isPublish, wysiwygEditor) {

        var title = $('#clog_title_field').val();

        if (title.length < 4) {
            if ('AUTOSAVE' !== visibility) {
                alert(short_title_warning);
            }
            return 0;
        }
        
        if (title.length > 255) {
            if ('AUTOSAVE' !== visibility) {
                alert(long_title_warning);
            }
            return 0;
        }

        var success = false;

        var groups = '';
        
        if ('READY' === visibility) {
            if ($('#clog_visibility_tutor').prop('checked')) {
                visibility = 'TUTOR';
            } else if ($('#clog_visibility_site').prop('checked')) {
                visibility = 'SITE';
            } else {
                visibility = 'GROUP';
                var groupsArray = $('#clog-group-selector').val();

                if (groupsArray == null || groupsArray.length == 0) {
                    return 0;
                } else {
                    groups = groupsArray.join();
                }
            }
        }

        var content = clog.sakai.getEditorData(wysiwygEditor, 'clog_content_editor');

        if ('' == content) {
            if ('AUTOSAVE' !== visibility) {
                alert(clog.i18n.no_content_warning);
            }
            return 0;
        }

        var post = {
            'id': $('#clog_post_id_field').val(),
            'visibility': visibility,
            'commentable': $('#clog_commentable_checkbox').attr('checked') === 'checked',
            'title': title,
            'content': content,
            'groups': groups,
            'siteId': clog.siteId,
            'mode': isPublish
        };
                
        jQuery.ajax( {
            url: '/direct/clog-post/store.json',
            type: 'POST',
            data: post,
            dataType: 'text',
            timeout: clog.AJAX_TIMEOUT,
            success: function (data) {

                if ('AUTOSAVE' === visibility) {
                    clog.currentPost.id = data;
                    $('#clog_post_id_field').val(data);
                    success = true;
                    clog.titleChanged = false;
                    clog.sakai.resetEditor(wysiwygEditor, 'clog_content_editor');
                    $('#clog_autosaved_message').show();
                    setTimeout(function () {
                            $('#clog_autosaved_message').fadeOut(200);
                        }, 2000);
                } else {
                    clog.groups = jQuery.parseJSON(data);
                    clog.utils.addFormattedLastPostDatesToGroups(clog.groups);
                    if ('GROUP' === visibility) {
                        clog.switchState('groups');
                    } else {
                        clog.switchState(clog.homeState);
                    }
                }
            },
            error : function(xmlHttpRequest, textStatus, error) {
                alert("Failed to store post. Status: " + textStatus + '. Error: ' + error);
            }
        });

        return success;
    },
    saveComment: function (wysiwygEditor) {
        
        var comment = {
                'id': $('#clog_comment_id_field').val(),
                'postId': clog.currentPost.id,
                'content': clog.sakai.getEditorData(wysiwygEditor,'clog_content_editor'),
                'siteId': clog.siteId
                };

        jQuery.ajax( {
            url: "/direct/clog-comment/new",
            type: 'POST',
            data: comment,
            dataType: 'text',
            timeout: clog.AJAX_TIMEOUT,
            success: function (id) {
                clog.switchState('viewAllPosts');
            },
            error : function (xmlHttpRequest, textStatus, error) {
                alert("Failed to save comment. Status: " + textStatus + '. Error: ' + error);
            }
        });

        return false;
    },
    deleteAutosavedCopy: function(postId) {

        if (postId === '') {
            // CLOG-19 The post hasn't been autosaved yet. If it had been it would have an id
            return;
        }

        jQuery.ajax( {
            url: "/direct/clog-post/" + postId + "/deleteAutosavedCopy",
            dataType: 'text',
            timeout: clog.AJAX_TIMEOUT,
            success: function (result) {
                clog.switchState('viewAllPosts');
            },
            error : function(xmlHttpRequest, textStatus, error) {
                alert("Failed to delete autosaved copy. Status: " + textStatus + '. Error: ' + error);
            }
        });
    },
    recyclePost: function (postId) {

        if (!confirm(clog.i18n.delete_post_message)) {
            return false;
        }

        jQuery.ajax( {
            url: "/direct/clog-post/" + postId + "/recycle",
            dataType: 'text',
            cache: false,
            timeout: clog.AJAX_TIMEOUT,
            success: function (result) {

                if (clog.states.GROUP_POSTS === clog.currentState) {
                    clog.switchState(clog.currentState, { groupId: clog.currentGroupId, groupTitle: clog.currentGroupTitle });
                } else {
                    clog.switchState(clog.currentState);
                }
            },
            error: function (xmlHttpRequest, textStatus, error) {
                alert("Failed to recycle post. Status: " + textStatus + '. Error: ' + error);
            }
        });

        return false;
    },
    deleteSelectedPosts: function () {
    
        if (!confirm(clog.i18n.really_delete_post_message)) {
            return false;
        }
        
        var selected = $('.clog_recycled_post_checkbox:checked');

        if (selected.length <= 0) {
            // No posts selected for deletion
            return;
        }

        var postIds = clog.utils.joinElementIds(selected);

        jQuery.ajax( {
            url: "/direct/clog-post/remove?posts=" + postIds + "&site=" + clog.siteId,
            dataType: 'text',
            timeout: clog.AJAX_TIMEOUT,
            success: function (result) {
                clog.switchState('viewAllPosts');
            },
            error : function (xmlHttpRequest, textStatus, error) {
                alert("Failed to delete selected posts. Status: " + textStatus + '. Error: ' + error);
            }
        });

        return false;
    },
    restoreSelectedPosts: function() {

        var selected = $('.clog_recycled_post_checkbox:checked');

        // CLOG-29
        if (selected.length <= 0) {
            // No posts selected for restoration
            return;
        }

        var postIds = clog.utils.joinElementIds(selected);

        jQuery.ajax( {
            url : "/direct/clog-post/restore?posts=" + postIds,
            dataType : 'text',
            cache: false,
            timeout: clog.AJAX_TIMEOUT,
            success : function (result) {
                clog.switchState('viewAllPosts');
            },
            error : function(xmlHttpRequest,status,error) {
                alert("Failed to restore selected posts. Status: " + status + '. Error: ' + error);
            }
        });

        return false;
    },
    findPost: function (postId, callback) {
        
        if (!clog.currentPosts) {

            jQuery.ajax( {
                url: "/direct/clog-post/" + postId + ".json",
                dataType: "json",
                cache: false,
                timeout: clog.AJAX_TIMEOUT,
                success: function (p, status) {
                    callback(p);
                },
                error : function (xmlHttpRequest,stat,error) {
                    alert("Failed to get the post. Status: " + stat + ". Error: " + error);
                }
            });
        } else {
            clog.currentPosts.forEach(function (p) {
                if (p.id === postId) {
                    callback(p);
                }
            });
        }
    },
    deleteComment: function (commentId) {
                        
        if (!confirm(clog.i18n.delete_comment_message)) {
            return false;
        }
        
        jQuery.ajax( {
            url: "/direct/clog-comment/" + commentId + "?siteId=" + clog.siteId,
            type:'DELETE',
            timeout: clog.AJAX_TIMEOUT,
            success: function (text, status) {
                clog.switchState('viewAllPosts');
            },
            error: function (xmlHttpRequest, textStatus, error) {
                alert("Failed to delete comment. Status: " + textStatus + ". Error: " + error);
            }
        });
        
        return false;
    },
    decoratePost: function (p) {

        p.isRecycled = 'RECYCLED' === p.visibility;
        p.canComment = clog.currentUserPermissions.commentCreate && p.commentable;
        p.canDelete = clog.userId === '!admin'
                        || clog.currentUserPermissions.postDeleteAny
                        || (clog.currentUserPermissions.postDeleteOwn
                            && p.creatorId === clog.userId);
        p.canEdit = clog.currentUserPermissions.postUpdateAny
                        || (clog.currentUserPermissions.postUpdateOwn && p.creatorId === clog.userId);
        p.isModified = p.modifiedDate > p.createdDate;

        p.comments.forEach(function (c) {

            c.modified = c.modifiedDate > c.createdDate;
            c.canDelete = clog.currentUserPermissions.commentDeleteAny
                            || (clog.currentUserPermissions.commentDeleteOwn
                                && c.creatorId === clog.userId);
            c.canEdit = clog.currentUserPermissions.commentUpdateAny
                            || (clog.currentUserPermissions.commentUpdateOwn
                                && c.creatorId === clog.userId);
        });
    },
    renderTemplate: function (name, data, output) {

        var template = Handlebars.templates[name];
        document.getElementById(output).innerHTML = template(data);
    },
    renderPost: function (post, output) {

        this.decoratePost(post);
        this.renderTemplate('post', post, output);
        if (typeof MathJax !== 'undefined') { MathJax.Hub.Queue(["Typeset",MathJax.Hub]); }
    },
    renderPageOfPosts: function (args) {

        var loadImage = $('#clog-loading-image')
        loadImage.show();

        var url = "/direct/clog-post/posts.json?siteId=" + clog.siteId + "&page=" + clog.page;

        if (args && args.userId) {
            url += '&creatorId=' + args.userId;
        }

        if (args && args.groupId) {
            url += '&groupId=' + args.groupId;
        }

        if (args && args.isPublic) {
            url += '&visibilities=PUBLIC';
        }

        $.ajax( {
            url : url,
            dataType: "json",
            cache: false,
            timeout: clog.AJAX_TIMEOUT,
            success: function (data) {

                if (data.status === 'END') {
                    $(window).off('scroll.clog');
                    loadImage.hide();
                } else {
                    $(window).off('scroll.clog').on('scroll.clog', clog.utils.getScrollFunction(args, clog.utils.renderPageOfPosts));
                }

                clog.postsTotal = data.postsTotal;
                var posts = data.posts;

                clog.currentPosts = clog.currentPosts.concat(posts);

                if (clog.page == 0) {
                    $('#clog-posts-total').html(data.postsTotal);
                    if (data.postsTotal > 0) {
                        $('#clog-body-toggle').show();
                    }
                }

                clog.postsRendered += posts.length;

                clog.utils.addFormattedDatesToPosts(posts);
                var t = Handlebars.templates['posts'];
                $('#clog-posts').append(t({ posts: posts }));

                $(document).ready(function () {

                    posts.forEach(function (p) {
                        clog.utils.renderPost(p, 'post_' + p.id);
                    });

                    $(document).ready(function () {

                        clog.utils.attachProfilePopup();
                    });

                    if (!clog.settings.showBody) {
                        $('.clog_body').hide();
                    }

                    loadImage.hide();
                });
                clog.page += 1;
            },
            error : function (xmlHttpRequest, textStatus, errorThrown) {
                alert("Failed to get posts. Reason: " + errorThrown);
            }
        });
    },
    renderPageOfMembers: function (args) {

        var loadImage = $('#clog-loading-image')
        loadImage.show();

        if (clog.page == 0) {
            $('#clog-authors').html('');
        }

        var url = '/direct/clog-author/authors.json?siteId=' + clog.siteId + '&page=' + clog.page;

        if (args && args.sort) {
            url += '&sort=' + args.sort;
        }

        jQuery.ajax({
            url: url,
            dataType: "json",
            cache: false,
            timeout: clog.AJAX_TIMEOUT,
            success: function (data) {

                if (data.status === 'END') {
                    $(window).off('scroll.clog');
                    loadImage.hide();
                } else {
                    $(window).off('scroll.clog').on('scroll.clog', clog.utils.getScrollFunction(args, clog.utils.renderPageOfMembers));
                }

                if (clog.page == 0) {
                    $('#clog-authors-total').html(data.authorsTotal);
                }

                var authors = data.authors;

                authors.forEach(function (a) {
                    a.formattedDateOfLastPost = clog.utils.formatDate(a.dateOfLastPost);
                });

                var t = Handlebars.templates['authors'];
                $('#clog-authors').append(t({ 'authors': authors }));

                $(document).ready(function () {

                    clog.utils.attachProfilePopup();
                    loadImage.hide();
                });

                clog.page += 1;
            },
            error : function (xmlHttpRequest, status, errorThrown) {
                alert("Failed to get authors. Reason: " + errorThrown);
            }
        });
    },
    checkScroll: function () {

        // Check if there is no scroll rendered and there are more pages

        // Check if body height is lower than window height (scrollbar missed, maybe you need to get more pages automatically)
        if ($("body").height() <= $(window).height()) {
            setTimeout(function () {

                if (clog.postsTotal > clog.postsRendered && clog.postsRendered > 0 && clog.postsRendered % 10 === 0) {
                    $("body").data("scroll-clog", true);
                    $(window).trigger('scroll.clog');
                }
            }, 100);
        }
    },
    getScrollFunction: function (args, callback) {

        var scroller = function () {
            
            var wintop = $(window).scrollTop(), docheight = $(document).height(), winheight = $(window).height();

            if  ((wintop/(docheight-winheight)) > 0.95 || $("body").data("scroll-clog") === true) {
                $("body").data("scroll-clog", false);
                $(window).off('scroll.clog');
                callback(args);
            }
        };

        return scroller;
    }
};

Handlebars.registerHelper('translate', function (key) {
    return clog.i18n[key];
});
