@param org.ethelred.temperature4.UIRequestContext req
@param gg.jte.Content body

<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="${req.contextPath()}style.css" />
    <script src="https://unpkg.com/htmx.org@2.0.3" integrity="sha384-0895/pl2MU10Hqc6jd4RvrthNlDiE9U1tWmX7WRESftEDRosgxNsQG/Ze9YMRzHq" crossorigin="anonymous"></script>
    <title>Heat Control${"".equals(req.title()) ? "" : " - " + req.title()}</title>
</head>
<body class="${req.rootClass()} App">
<h1>${"".equals(req.title()) ? "Heat Control" : req.title()}
    <form method="post" action="${req.contextPath()}preferences/unit">
        <button type="submit">°${req.temperatureUnit().symbol()}</button>
    </form>
</h1>
<div id="content">
${body}
</div>
</body>
</html>