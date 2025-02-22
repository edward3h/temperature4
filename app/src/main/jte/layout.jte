@param String title
@param String rootClass
@param gg.jte.Content body

<html>
<head>
    <link rel="stylesheet" href="/style.css" />
    <script src="https://unpkg.com/htmx.org@2.0.3" integrity="sha384-0895/pl2MU10Hqc6jd4RvrthNlDiE9U1tWmX7WRESftEDRosgxNsQG/Ze9YMRzHq" crossorigin="anonymous"></script>
    <title>Heat Control${"".equals(title) ? "" : " - " + title}</title>
</head>
<body class="${rootClass} App">
<h1>${"".equals(title) ? "Heat Control" : title}</h1>
<div id="content">
${body}
</div>
</body>
</html>