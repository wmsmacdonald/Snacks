'use strict';

$('#submit_form').on('submit', function(e) {
    // prevent from reloading page
    e.preventDefault();
    let suggestionInput = $('#suggestionInput').val();
    let suggestionLocation = $('#suggestionLocation').val();
    let data, url;
    // user is submitting an existing snack
    if (suggestionInput === "" && suggestionLocation === "") {
        url = '/snacks/suggest_existing';
        data = {
            "id": $('#snackOptions').val()
        }
    }
    else {
        url = '/snacks/suggest_new';
        data = {
            "name": suggestionInput,
            "location": suggestionLocation
        }
    }
    $.ajax({
        url,
        method: "post",
        dataType: "json",
        data
    }).then(data => {
        if (data.error) {
            alert('Cannot suggest: ' + data.error);
        }
        else {
            window.location = "/";
        }
    })
});
