(function() {
    'use strict';

    Array.prototype.clean = function(deleteValue) {
        for (var i = 0; i < this.length; i++) {
            if (this[i] === deleteValue) {
                this.splice(i, 1);
                i--;
            }
        }
        return this;
    };

    if(!document.getElementsByClassName || !document.body.addEventListener) {return;}

    var rules = {
        WordCount: function WordCount(value, config) {
             var w = value.split(/\s+/).clean('').length;
             var words = function(count) {return count === 1 ? 'word' : 'words';};

             return w === 0  ? '' + config.maxWords + ' ' + words(config.maxWords) + ' maximum'
                  : w <= config.maxWords ? 'Words remaining: ' + (config.maxWords - w)
                  : '' + (w - config.maxWords) + ' ' + words(w - config.maxWords) + ' over limit';
        }
    };

    function addInputListener(input, helper, rule, config) {
        // TODO: get this working in IE8
        if (!input || !rule) {return;}

        input.addEventListener('keyup', function() {
            var output = rule(input.value, config);
            helper.innerHTML = output;
        });

        input.addEventListener('paste', function() {
            // The paste event fires before the element has received the
            // pasted text.
            window.setTimeout(function() {
                var output = rule(input.value, config);
                helper.innerHTML = output;
            }, 200);
        });

        input.addEventListener('drop', function() {
            // drop events happen before the drop event has completed
            // so we need this hack to wait for the drop event to complete.
            window.setTimeout(function() {
                var output = rule(input.value, config);
                helper.innerHTML = output;
            }, 200);
        });
    }

    function rifsHelperText() {
        var helpers = document.getElementsByClassName('js__hint');
        for (var i = 0; i < helpers.length; i++) {
            var helper = helpers[i];
            var input = document.getElementById(helper.getAttribute('data-for')),
                rule = rules[helper.getAttribute('data-rule')] || null,
                config = JSON.parse(helper.getAttribute('data-ruleconfig') || '{}');

            addInputListener(input, helper, rule, config);
        }
    }

    window.rifsHelperText = rifsHelperText;

    rifsHelperText();

}());

$(document).ready(function () {
    jQuery.fx.off = true;
    var GOVUK = window.GOVUK || {};

    // Don't enhance the selection buttons on IE8 as it can't handle the javascript.
    if (navigator.appVersion.indexOf('MSIE 8.') === -1) {
        var selectionButtons = new GOVUK.SelectionButtons($('.block-label input[type="radio"], .block-label input[type="checkbox"]'));
    }

    // Turn the tabs on if the correct structures exist in the page
    var e = $('section.more');
    e.find('.js-tabs').length && e.tabs();

    // $('details').details();

    $('.js-hide-on-load').hide();

    // Trigger Show/Hide events
    $('.js-show').click(function(){
       var selector = $(this).attr('data-for');
       var el = $(selector);
       el.show();
    });

    $('.js-hide').click(function(){
        var selector = $(this).attr('data-for');
        var el = $(selector);
        el.hide();
    });

    $('.file-upload-field').change(function () {
        var filename = $(this).val();
        var lastIndex = filename.lastIndexOf("\\");
        if (lastIndex >= 0) {
          filename = filename.substring(lastIndex + 1);
        }
        $('#fileupload').val(filename);
    });



    $('.js-navigation-toggle').click(function(){

        // A selector for the element to toggle is stashed in the data-for attribute
        var selector = $(this).attr('data-for');
        var el = $(selector);

        if (el.is(':visible')) {
            // Changes on the <a> link driving this
            $(this)
                .removeClass('show-all-parts-open')
                .addClass('show-all-parts-closed');

            // Change the verb of the link
            $(this).text($(this).text().replace('Hide', 'Show'));

            // Handle presentation changes to the navigation container
            el.removeClass('nav-open').addClass('nav-closed');
        } else {

            // Changes on the <a> link driving this
            $(this)
                .removeClass('show-all-parts-closed')
                .addClass('show-all-parts-open');

            // Change the verb of the link
            $(this).text($(this).text().replace('Show', 'Hide'));

            // Handle presentation changes to the navigation container
            el
                .removeClass('nav-closed')
                .addClass('nav-open');
        }
    });
});