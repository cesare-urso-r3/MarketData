"use strict";

function getKnownPermissions () {
    $.getJSON("http://localhost:10050/permission", function (data) {

      let body = $('#knownPermissions tbody');
      body.empty();

      $.each(data.permissions, function(index, val) {
          let that = this;
          body.append(
              $("<tr/>")
                  .append(
                      $("<td/>").text(this.dataSet),
                      $("<td/>").text(this.provider),
                      $("<td/>").text(this.subscriber),
                      $("<td/>").text(this.dataChargeOwner),
                      $("<td/>").append(
                          $("<button/>")
                          .text("Use")
                          .click(function (event) {
                            $(this).addClass("button-primary")
                              let btn = $(this)
                              var posting = $.post( "http://localhost:10050/usage", {
                                  "dataSet": that.dataSet,
                                  "provider" : that.provider,
                                  "dataChargeOwner" : that.dataChargeOwner },
                                  function (data) {
                                      btn.removeClass("button-primary")
                                  })
                              })
                          ),
                      $("<td/>").append(
                          $("<button/>")
                          .text("Check Usages")
                          .click(function (event) {
                              var getting = $.getJSON( "http://localhost:10050/usage", {
                                  "dataSet": that.dataSet,
                                  "provider" : that.provider,
                                  "dataChargeOwner" : that.dataChargeOwner } );

                              getting.done(function( data ) {
                                  $("#main").append(
                                      $("<div id='usages' class='modal'/>").append(
                                          $("<div id='usagesList' class='modalContent'/>").append(
                                              $("<button/>")
                                              .text("Close")
                                              .click(function (event) {
                                                  $('#usages').remove();
                                              })
                                          )
                                    )
                                );
                                $.each(data.usages, function(index, val) {
                                    $("<p/>").text(val.date).prependTo($("#usagesList"))
                                })

                              });
                          })
                      )
                  )
              )
         });
      }).fail(function (jqXHR, textStatus, errorThrown) { console.log(textStatus); console.log(errorThrown);});
}

$(function(){

    let main = $("#main")
    main.append(
    $("<h4>").text("Request Permission for Data Set"),

      $("<h4>").text("Request Permission for Data Set"),
      $("<form id='theForm'/>").append(
          $("<div width='100px'/>").text("Data Set"),
          $("<input/>")
          .attr("placeholder", "data set")
          .attr("type", "text")
          .attr("id", "dataSet"),
          $("<br/>"),
          $("<div width='100px'/>").text("Provider"),
          $("<select/>")
          .attr("id", "provider")
          .attr("class", "party"),
          $("<br/>"),

          $("<div width='100px'/>").text("Redistributor"),
          $("<select/>")
          .attr("id", "redistributor")
          .attr("class", "party"),

          $("<br/>"),
          $("<button/>")
          .text("Go")
          .click(function (event) {
              event.preventDefault()

            // Get some values from elements on the page:
            let $form = $("#theForm"),
             dataSet = $("#dataSet").val(),
             provider = $('#provider :selected').text(),
             redistributor = $('#redistributor :selected').text();

              // Send the data using post
              var posting = $.post( "http://localhost:10050/permission", {
                  "dataSet": dataSet,
                  "provider" : provider,
                  "redistributor" : redistributor } );

              posting.done(function( data ) {
                setTimeout( () => getKnownPermissions(),1000)
              });

          } )
      ),
      $("<h4>").text("Known Permissions"),
      $("<table id=\"knownPermissions\">").append(
          $("<thead />").append(
              $("<th/>").text("Data Set"),
              $("<th/>").text("Provider"),
              $("<th/>").text("Subscriber"),
              $("<th/>").text("Data Charge Owner"),
              $("<th/>")
          ),
          $("<tbody />")
      ),
    );
    $.getJSON("http://localhost:10050/party", function (data) {

      $.each(data.parties, function(index, val) {

          $(".party").append($('<option>').val(val).text(val))
      })
  }).fail(function (jqXHR, textStatus, errorThrown) { console.log(textStatus); console.log(errorThrown);});

  getKnownPermissions();


});
// Define your client-side logic here.
