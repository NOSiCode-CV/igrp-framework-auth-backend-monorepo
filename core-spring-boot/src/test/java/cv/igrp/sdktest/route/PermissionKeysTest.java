package cv.igrp.sdktest.route;

import cv.igrp.framework.auth.core.route.PermissionKeys;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The key convention ({@code {service}.{resource path}.{action}}) as a table:
 * one route in, one key out.
 */
class PermissionKeysTest {

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource(delimiter = '|', textBlock = """
            # leading api and vN removed, only at the start
            GET     | /api/v1/faturas/{id}/linhas     | faturacao-api.faturas.linhas.list
            GET     | /api/faturas                    | faturacao-api.faturas.list
            GET     | /v2/faturas                     | faturacao-api.faturas.list
            GET     | /faturas/api/v1                 | faturacao-api.faturas.api.v1.list
            GET     | /api/v1/api                     | faturacao-api.api.list
            GET     | /apis/faturas                   | faturacao-api.apis.faturas.list
            GET     | /version/faturas                | faturacao-api.version.faturas.list
            # path variables dropped, nested resources kept in order
            POST    | /faturas/{id}/linhas            | faturacao-api.faturas.linhas.create
            PATCH   | /faturas/{id}/linhas/{lid}      | faturacao-api.faturas.linhas.patch
            POST    | /faturas/{id}/aprovar           | faturacao-api.faturas.aprovar.create
            # root path
            GET     | /                               | faturacao-api.root.list
            GET     | ''                              | faturacao-api.root.list
            GET     | /api/v1                         | faturacao-api.root.list
            # method / ending table
            GET     | /faturas/{id}                   | faturacao-api.faturas.read
            POST    | /faturas                        | faturacao-api.faturas.create
            POST    | /faturas/{id}                   | faturacao-api.faturas.create
            PUT     | /faturas                        | faturacao-api.faturas.update-all
            PUT     | /faturas/{id}                   | faturacao-api.faturas.update
            PATCH   | /faturas                        | faturacao-api.faturas.patch-all
            PATCH   | /faturas/{id}                   | faturacao-api.faturas.patch
            DELETE  | /faturas                        | faturacao-api.faturas.delete-all
            DELETE  | /faturas/{id}                   | faturacao-api.faturas.delete
            HEAD    | /faturas                        | faturacao-api.faturas.head
            OPTIONS | /faturas/{id}                   | faturacao-api.faturas.options
            # patterns as Spring reports them: regex variables, catch-all, wildcards
            GET     | /faturas/{id:\\d+}              | faturacao-api.faturas.read
            GET     | /faturas/{id:\\d{3}}/linhas     | faturacao-api.faturas.linhas.list
            GET     | /ficheiros/{*caminho}           | faturacao-api.ficheiros.read
            GET     | /estaticos/**                   | faturacao-api.estaticos.read
            GET     | /estaticos/*                    | faturacao-api.estaticos.read
            # a variable inside a segment: the literal part stays, the route reads one item
            GET     | /relatorio-{ano}                | faturacao-api.relatorio.read
            GET     | /ficheiros/*.pdf                | faturacao-api.ficheiros.pdf.read
            # a dot inside a segment never adds a key segment
            GET     | /v1/export.json                 | faturacao-api.export-json.list
            # the method may arrive in any case
            get     | /faturas                        | faturacao-api.faturas.list
            # a trailing slash does not change the key
            GET     | /faturas/                       | faturacao-api.faturas.list
            """)
    void derivesKeyFromRoute(String method, String path, String expectedKey) {
        assertEquals(expectedKey, PermissionKeys.derive("faturacao-api", method, path));
    }
}
