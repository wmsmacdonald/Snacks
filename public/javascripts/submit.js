'use strict';

$('#submit_form').on('submit', function(e) {
    e.preventDefault();
    $.ajax({
        url: '/snacks/create',
        method: "post",
        dataType: "json",
        data: {
            "name": $('#suggestionInput').val(),
            "location": $('#suggestionLocation').val()
        }
    }).then(data => {
        if (data.error) {
            alert('Cannot suggest: ' + data.error);
        }
        else {
            window.location = "/";
        }
    })
});
