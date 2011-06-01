/**
 * Copyright (C) 2010-2011 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var display;
var canvas;
var context;
var overlay;
var overlayContext;
var underlay;
var underlayContext;
var scale;
var scaleContext;
var buckets = [];
var maxColumns = 96;
var maxRows = 10;
var gridSize = 30;
var gutterSize = 24;

var rightLimit = 0;
var selectedBucket;

function initCanvas() {
	display = document.getElementById("display");

	canvas = document.getElementById("canvas");
	context = canvas.getContext("2d");
	overlay = document.getElementById("overlay");
	overlayContext = overlay.getContext("2d");

	underlay = document.getElementById("underlay");
	underlayContext = underlay.getContext("2d");

	scale = document.getElementById("scale");
	scaleContext = scale.getContext("2d");

	rightLimit = (maxColumns * gridSize) - canvas.width;
}

function clearCanvas() {
	canvas.width = canvas.width;
}

function clearOverlay() {
	overlay.width = overlay.width;
}

function clearUnderlay() {
	underlay.width = underlay.width;
}

function clearScale() {
	scale.width = scale.width;
}

var sessionId = null;

function createSession() {
	var handleSessionId = function(data, status, req) {
		var location = req.getResponseHeader('Location');
		var parts = location.split("/");
		var sessionID = parts[parts.length - 1];
		sessionId = sessionID;
	};
	$.post(API_BASE + '/diffs/sessions', {}, handleSessionId, "json");
}

var startTime, endTime;
var bucketSize = 3600;

const TIME_FORMAT = "yyyyMMddTHHmmssZ";
var swimlaneLabels = [];

function nearestHour() {
	var hours = (new Date()).getHours() + 1;
	return Date.today().add({hours: hours});
}
function loadBuckets() {
	buckets = [];
	for (var i = 0; i < maxRows; i++) {
		var row = [];
		for (var j = 0; j < maxColumns; j++) {
			row[j] = 0;
		}
		buckets[i] = row;
	}

	endTime = nearestHour();

	var now = endTime.toString(TIME_FORMAT);

	startTime = endTime.add({hours: -1 * maxColumns});
	var dayBeforeNow = startTime.toString(TIME_FORMAT);

	$.get("rest/diffs/sessions/" + sessionId + "/zoom?range-start=" + dayBeforeNow + "&range-end=" + now + "&bucketing=3600", function(data) {
		var indexer = 0;

		for (var pair in data) {
			swimlaneLabels[indexer] = pair;
			for (var x = 0; x < data[pair].length; x++) {
				buckets[indexer][x] = data[pair][x];
			}
			indexer++;
		}
		clearEverything();
		o_x = -1 * rightLimit;
		context.translate(o_x, o_y);
		scaleContext.translate(o_x, o_y);
		drawGrid();
	});
}

function renderActions() {
	var $actionListContainer = $("#actionlist").empty();
	var actionListCallback = function(actionList, status, xhr) {
		if (!actionList) {
			return;
		}
		var $repairStatus = $('#repairstatus');
		$.each(actionList, function(i, action) {

			$("<label>" + action.name + "</label>").appendTo($actionListContainer);
			$('<button class="repair">Go</button>')
				.click(function(e) {
					e.preventDefault();
					var $button = $(this),
						url = API_BASE + action.action.replace("${id}", itemID);
					if ($button.hasClass('disabled')) {
						return false;
					}
					$button.addClass('disabled');
					$repairStatus.text('Repairing...');
					$.ajax({
							type: action.method,
							url: url,
							success: function(data, status, xhr) {
								$repairStatus.html('Repair status: ' + data.result + '<br/>output: ' + data.output);
							},
							error: function(xhr, status, ex) {
								if (console && console.log) {
									var error = {
										type: action.method,
										url: url,
										status: status,
										exception: ex,
										xhr: xhr
									};
									console.log("error during repair for item " + itemID + ": ", error);
								}
								$repairStatus.text('Error during repair: ' + (status || ex.message));
							},
							complete: function() {
								$button.removeClass('disabled');
							}
						});
					return false;
				})
				.appendTo($actionListContainer);
			$('<br class="clearboth"/>').appendTo($actionListContainer);
		});
	};

	$.ajax({ url: API_BASE + '/actions/' + pairKey, success: actionListCallback });
}

function renderEvent(event) {
	if (event != null) {
		var itemID = event.objId.id,
			pairKey = event.objId.pairKey,
			seqID = event.seqId,
			upstreamLabel = "upstream",
			upstreamVersion = event.upstreamVsn || "no version",
			downstreamLabel = "downstream",
			downstreamVersion = event.downstreamVsn || "no version";

		$('#contentviewer h6').eq(0).text('Content for item ID: ' + itemID);
		$('#item1 .diffHash').html('<span>' + upstreamLabel + '</span>' + upstreamVersion);
		$('#item2 .diffHash').html('<span>' + downstreamLabel + '</span>' + downstreamVersion);

		var getContent = function(selector, label, upOrDown) {
			$.ajax({
					url: "rest/diffs/events/" + sessionId + "/" + seqID + "/" + upOrDown,
					success: function(data) {
						$(selector).text(data || "no content found for " + upOrDown);
					},
					error: function(xhr, status, ex) {
						if (console && console.log) {
							console.log('error getting the content for ' + (label || "(no label)"), status, ex, xhr);
						}
					}
				});
		};

		$.get("rest/config/pairs/" + pairKey, function(data, status, xhr) {
			upstreamLabel = data.upstream.name;
			$("#item1 h6").text(upstreamLabel);
			downstreamLabel = data.downstream.name;
			$("#item2 h6").text(downstreamLabel);
			getContent("#item1 pre", upstreamLabel, "upstream");
			getContent("#item2 pre", downstreamLabel, "downstream");
		});
	}
}

function selectFromList(event) {
	if (!event) {
		return false;
	}
	var row = event.target.nodeName === "tr" ? $(event.target) : $(event.target).closest('tr');
	renderEvent(row.data("event"));
	if (row.data("event") != null) {
		$('#diffList').find('tbody tr').removeClass("specific_selected");
		$('#evt_' + row.data("event").seqId).addClass("specific_selected");
	}
}

function addRow(table, event) {
	var time = new Date(event.detectedAt).toString("HH:mm:ss");
	var date = new Date(event.detectedAt).toString("dd/MM/yyyy");
	var row = $("<tr id='evt_" + event.seqId + "'></tr>")
		.append("<td class='date'>" + date + "</td>")
		.append("<td>" + time + "</td>")
		/*.append("<td id='" + event.detectedAt + "_" + event.objId.pairKey + "_group'></td>")*/
		.append("<td>" + event.objId.pairKey + "</td>")
		.append("<td>" + event.objId.id + "</td>")
		.data("event", event);

	if (!event.upstreamVsn) {
		row.append("<td>Missing from upstream</td>");
	}
	else if (!event.downstreamVsn) {
		row.append("<td>Missing from downstream</td>");
	}
	else {
		row.append("<td>Data difference</td>");
	}

	table.append(row);
}

var listSize = 20;
var page = 0;
function previous() {
	if (page > 0) {
		page--;
	}
	fetchData();
}

var itemCount = 0;
function next() {
	if (selectedBucket != null && buckets[selectedBucket.row] != null) {
		if ((page + 1) * listSize < itemCount) {
			page++;
			fetchData();
		}
	}
}

function fetchData() {
	itemCount = 0;
	if (selectedBucket != null && buckets[selectedBucket.row] != null) {
		if (buckets[selectedBucket.row][selectedBucket.column] > 0) {
			for (var i = 0; i < maxRows; i++) {
				itemCount += buckets[i][selectedBucket.column];
			}
			var selectedStart = new Date(startTime.getTime() + (selectedBucket.column * bucketSize * 1000));
			var selectedEnd = new Date(selectedStart.getTime() + (bucketSize * 1000));

			var url = "rest/diffs/sessions/" + sessionId + "/page?range-start="
				+ selectedStart.toString(TIME_FORMAT) + "&range-end=" + selectedEnd.toString(TIME_FORMAT)
				+ "&offset=" + (page * listSize) + "&length=" + listSize;

			$.get(url, function(data) {
				renderEvent(data[0]);
				var list = $('#diffList').find('tbody').empty().end();
				$.each(data, function(i, event) {
					addRow(list, event);
				});
			});
			$("#pagecount").text("Page " + (page + 1) + " of " + Math.ceil(itemCount / listSize));
			$("#navigation").show();
		}
	}
}

var timeout;
var polling = true;
function startPolling() {
	polling = true;
	clearTimeout(timeout);
	loadBuckets();
	timeout = window.setTimeout(startPolling, 5000);
	$("#polling").text("Stop polling");
}

function stopPolling() {
	polling = false;
	clearTimeout(timeout);
	$("#polling").text("Start polling");
}

function dashedLine(ctx, x1, y1, x2, y2, dashLen) {
	if (dashLen == undefined) dashLen = 2;

	ctx.beginPath();
	ctx.moveTo(x1, y1);

	var dX = x2 - x1;
	var dY = y2 - y1;
	var dashes = Math.floor(Math.sqrt(dX * dX + dY * dY) / dashLen);
	var dashX = dX / dashes;
	var dashY = dY / dashes;

	var q = 0;
	while (q++ < dashes) {
		x1 += dashX;
		y1 += dashY;
		if (q % 2 == 0) {
			ctx.moveTo(x1, y1);
		}
		else {
			ctx.lineTo(x1, y1);
		}
	}
	if (q % 2 == 0) {
		ctx.moveTo(x1, y1);
	}
	else {
		ctx.lineTo(x1, y1);
	}

	ctx.stroke();
	ctx.closePath();
}

function drawCircle(i, j) {
	var cell = coordsToPosition({"x":i, "y":j});
	if (cell.column < maxColumns && cell.row < maxRows) {
		var cell_x = i + Math.floor(gridSize / 2);
		var cell_y = j + gutterSize + Math.floor(gridSize / 2);
		var size = limit(buckets[cell.row][cell.column], Math.floor((gridSize - 1) / 2));
		if (size.limited) {
			context.lineWidth = 2;
		}
		else {
			context.lineWidth = 1;
		}
		context.strokeStyle = "black";
		context.fillStyle = "white";
		context.beginPath();
		context.arc(cell_x, cell_y, size.value, 0, Math.PI * 2, false);
		context.closePath();
		context.stroke();
		context.fill();
	}
}

var toggleX, toggleY;
var show_grid = false;
function drawGrid() {
	var region_width = maxColumns * gridSize;
	if (show_grid) {
		for (var x = 0.5; x < region_width; x += gridSize) {
			context.moveTo(x, 0);
			context.lineTo(x, canvas.height);
		}
		for (var y = 0.5; y < canvas.height; y += (2 * gutterSize + gridSize)) {
			context.moveTo(0, y);
			context.lineTo(region_width, y);
		}
		context.strokeStyle = "red";
		context.stroke();
	}

	var lane = 0;
	for (var s = 0.5 + (2 * gutterSize + gridSize); s < canvas.height; s += (2 * gutterSize + gridSize)) {
		dashedLine(underlayContext, 0, s, canvas.width, s, 2);
		if (swimlaneLabels[lane] != null) {
			underlayContext.font = "11px 'Lucida Grande', Tahoma, Arial, Verdana, sans-serif";
			underlayContext.fillText(swimlaneLabels[lane], 10, s - (2 * gutterSize + gridSize) + 12);
		}
		lane++;
	}

	if (polling) {
		var pollText = " LIVE ";
	}
	else {
		pollText = " CLICK TO POLL ";
	}
	var textWidth = underlayContext.measureText(pollText).width;
	var textSpacer = 20;
	underlayContext.fillStyle = "#d12f19";
	underlayContext.fillRect(canvas.width - textWidth - textSpacer, 0, textWidth + textSpacer, 20);
	underlayContext.fillStyle = "#fff";
	underlayContext.font = "12px 'Lucida Grande', Tahoma, Arial, Verdana, sans-serif"
	underlayContext.textBaseline = "top";
	underlayContext.fillText(pollText, canvas.width - underlayContext.measureText(pollText).width - (textSpacer / 2), 5);
	toggleX = canvas.width - textWidth - textSpacer;
	toggleY = 20;


	for (var i = 0.5; i < region_width; i += gridSize) {
		for (var j = 0.5; j < canvas.height; j += (2 * gutterSize + gridSize)) {
			drawCircle(i, j);
		}
	}

	scaleContext.font = "9px sans-serif";
	for (var sc = 0; sc < maxColumns; sc++) {
		if (sc % 3 == 0) {
			var tick = new Date(startTime.getTime() + (sc * bucketSize * 1000));
			scaleContext.fillText(tick.toString("dd/MM"), sc * gridSize, 10);
			scaleContext.fillText(tick.toString("HH:mm"), sc * gridSize, 20);
		}
	}
}

var highlighted;
function drawOverlay() {
	if (highlighted != null && highlighted.column >= 0 && highlighted.row >= 0) {
		var value = buckets[highlighted.row][highlighted.column];
		if (value > 0) {
			var c_x = highlighted.column * gridSize;
			var c_y = (highlighted.row * (2 * gutterSize + gridSize)) + gutterSize + gridSize;
			overlayContext.font = "12px sans-serif";
			overlayContext.textBaseline = "top";
			var width = context.measureText("" + value).width;
			overlayContext.fillText(value, c_x + Math.floor(gridSize / 2) - Math.floor(width / 2), c_y);
		}
	}
}

function limit(value, maximum) {
	if (value <= maximum) {
		return {"value":value, "limited":false};
	}
	return {"value":maximum, "limited":true};
}

var o_x = rightLimit;
var o_y = 0;
function coords(e) {
	var x;
	var y;
	if (e.pageX != undefined && e.pageY != undefined) {
		x = e.pageX;
		y = e.pageY;
	}
	else {
		x = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
		y = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
	}

	x -= display.offsetLeft;
	y -= display.offsetTop;

	return { "x":x, "y":y };
}

function coordsToPosition(coords) {
	return {
		"row": Math.floor(coords.y / (2 * gutterSize + gridSize)),
		"column": Math.floor((coords.x) / gridSize)
	};
}

function clearEverything() {
	clearCanvas();
	clearOverlay();
	clearUnderlay();
	clearScale();
}

var dragging = false;
var dragged = false;
function mouseDown(e) {
	dragging = e;
	dragged = false;
}

function togglePolling(c) {
	if (c.x > toggleX && c.y < toggleY) {
		if (polling) {
			stopPolling();
		}
		else {
			startPolling();
		}
	}
}

function mouseUp(e) {
	dragging = false;
	if (!dragged) {
		if (e.target.tagName == "CANVAS") {
			var c = coords(e);
			togglePolling(c);
			c.x -= o_x;
			selectedBucket = coordsToPosition(c);
			page = 0;
			fetchData();
		}
	}
	dragged = false;
}

function mouseMove(e) {
	if (dragging) {
		stopPolling();
		dragged = true;
		clearEverything();
		var m_coords = coords(e);
		var d_coords = coords(dragging);
		o_x += m_coords.x - d_coords.x;
		if (o_x > 0) {
			o_x = 0;
		}

		if (Math.abs(o_x) > rightLimit) {
			o_x = -1 * rightLimit;
		}
		context.translate(o_x, o_y);
		scaleContext.translate(o_x, 0);
		drawGrid();
		dragging = e;
	}
	else {
		clearOverlay();
		overlayContext.translate(o_x, o_y);
		mouseOver(e);
	}
}

function mouseOver(e) {
	var c = coords(e);
	c.x -= o_x;
	var position = coordsToPosition(c);
	if (position.row >= 0 && position.row < maxRows && position.column >= 0 && position.column < maxColumns) {
		highlighted = position;
		drawOverlay();
	}
}

function initGraph() {
	createSession();
	initCanvas();
	startPolling();

	$(document).mouseup(mouseUp);
	$("#display").mousedown(mouseDown);
	$(document).mousemove(mouseMove);


	$("#display").bind("contextmenu", function(e) {
		return false;
	});
	$("#diffList").click(function(e) {
		selectFromList(e);
	});

	$("#next").click(function(e) {
		next();
	});

	$("#previous").click(function(e) {
		previous();
	});

	$("#navigation").hide();

}