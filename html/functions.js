/*
 * CONSTANT DEFINITIONS
 */
// The ID of the tree input element.
var treeInputID = "treeText";
// The ID of the clipboard input element.
var clipInputID = "clipText";
// The ID of the node info element.
var nodeInfoID = "nodeText";

/*
function log(newtext)
{
 getObject("log").value = getObject("log").value +  newtext;
}
*/

function log(newtext)
{
  getObject("log").innerHTML += newtext;
  return;
}


/*
 * This function is called by Java to update the HTML's tree text.
 */
function updateTree(newtext)
{
  getObject(treeInputID).value = newtext;
  return;
}

function updateClip(newtext)
{
  getObject(clipInputID).value = newtext;
  return;
}

function updateNode(newtext)
{
  getObject(nodeInfoID).innerHTML = newtext;
//  getObject("PhyloWidget").focus();
}

/*
 * This function calls Java's updateTree method to update PhyloWidget's
 * representation of the tree.
 */
function updateJavaTree()
{
  document.PhyloWidget.updateTree(getObject(treeInputID).value);
}

/*
 *  This function calls Java's updateClip method to update PhyloWidget's
 *  tree clipboard.
 */
function updateJavaClip()
{
  document.PhyloWidget.updateClip(getObject(clipInputID).value);
}

// Wrapper function to get an object from the DOM via its ID attribute.
function getObject(id) {
	var el = document.getElementById(id);
	return (el);
}


/*
 *  This function causes the newick input box to be selected fully once.
 */
var selected=false;
function selectOnce(el)
{
  if (selected)return;
  el.select();
  selected = true;
  document.getElementById("")
}

var simple="((a,b),c);";
var noname="(,(,,),);";
var greek="(Alpha,Beta,Gamma,Delta,,Epsilon,,,);";