'use strict';

$('#submit_form').on('submit', function(e) {
    e.preventDefault();
    console.log('sdfasd')
    $.ajax({
        url: '/snacks/create',
        method: "post",
        data: {
            "name": $('#suggestionInput'),
            "location": $('#suggestionLocation')
        }
    }).then((data) => {
        console.log('sadfas')
       console.log(data);
    })
});
