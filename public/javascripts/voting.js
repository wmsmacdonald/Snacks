'use strict';

$(function() {
    // set the starting vote counter
    setVotesCounter(remainingVotes());

    // get only buttons for snacks that have been voted
    $('.vote_button').filter(function() {
        let id = $(this).data('id').toString();
        let voted = getVoted();
        return voted === undefined
            ? false
            : getVoted().indexOf(id) !== -1;
    }).each(function() {
        $(this).children().first().removeClass('icon-check_noVote').addClass('icon-check_voted');
    });

    $('.vote_button').click(function () {
        let id = $(this).data('id');
        let votePromise = vote(id);
        votePromise.then((err) => {
            if (err) {
                alert(err);
            }
            else {
                $(this).find('i').removeClass('icon-check_noVote');
                $(this).find('i').addClass('icon-check_voted');
                $(this).parent().prev().html((i, votes) => parseInt(votes) + 1);
                decrementVoteCounter();
            }
        })
    });

    function vote(id) {
        return $.getJSON('/snacks/vote', { id }).then(data => {
            if (data.error !== false) {
                return 'Cannot vote: ' + data.error
            }
            else {
                return false;
            }
        })
    }

    function remainingVotes() {
        let voted = getVoted();
        return voted === undefined
            // 3 remaining if cookie doesn't exist
            ? 3
            // otherwise calculate the remaining
            : 3 - parseInt(voted.length);
    }

    function getVoted() {
        let cookie = getCookie('votedFor');
        return cookie === undefined
            ? undefined
            : cookie.split(':')
    }

    function decrementVoteCounter() {
        setVotesCounter(remainingVotes());
    }

    function getCookie(name) {
        var value = "; " + document.cookie;
        var parts = value.split("; " + name + "=");
        if (parts.length == 2) return parts.pop().split(";").shift();
    }

    function setVotesCounter(remainingVotes) {
        if(remainingVotes == 0) {
            // change icon to red and set number of remaining
            $('.counter').text(remainingVotes).attr('class', 'counter counter_red');
        }
        if(remainingVotes == 1) {
            // change icon to yellow and set number of remaining
            $('.counter').text(remainingVotes).attr('class', 'counter counter_yellow');
        }
        if(remainingVotes > 1) {
            // change icon to green and set number of remaining
            $('.counter').text(remainingVotes).attr('class', 'counter counter_green');
        }
    }
});