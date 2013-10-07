
import br.bireme.tb.URLS;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author heitor
 */
public class teste {
     public static String executePost(String targetURL, String urlParameters)
  {
    URL url;
    HttpURLConnection connection = null;
    try {
      //Create connection
      url = new URL(targetURL);
      connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type",
           "application/x-www-form-urlencoded");

      connection.setRequestProperty("Content-Length", "" +
               Integer.toString(urlParameters.getBytes(URLS.DEFAULT_ENCODING).length));
      connection.setRequestProperty("Content-Language", "en-US");
      //connection.setRequestProperty("Accept-Charset", URLS.DEFAULT_ENCODING);

      connection.setUseCaches (false);
      connection.setDoInput(true);
      connection.setDoOutput(true);

      //Send request
      DataOutputStream wr = new DataOutputStream (
                  connection.getOutputStream ());
      wr.writeBytes (urlParameters);
      wr.flush ();
      wr.close ();

      //Get Response
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      String line;
      StringBuilder response = new StringBuilder();
      while((line = rd.readLine()) != null) {
        response.append(line);
        response.append('\r');
      }
      rd.close();
      return response.toString();

    } catch (Exception e) {

      e.printStackTrace();
      return null;

    } finally {

      if(connection != null) {
        connection.disconnect();
      }
    }
  }

     public static void main(final String[] args) throws UnsupportedEncodingException, IOException {
         /*final String targetURL = "http://tabnet.datasus.gov.br/cgi/tabnet.exe?idb2011/g01.def";
         //final String urlParameters = "Linha=Regi%E3o_%28capitais%29&Coluna=--N%E3o-Ativa--&Incremento=Preval%EAncia_diabete_melito&Arquivos=vigtel10.dbf&SRegi%E3o_%28capitais%29=TODAS_AS_CATEGORIAS__&SCapital=TODAS_AS_CATEGORIAS__&SSexo=TODAS_AS_CATEGORIAS__&SFaixa_et%E1ria=TODAS_AS_CATEGORIAS__&SEscolaridade=TODAS_AS_CATEGORIAS__&mostre=Mostra";
         final String urlParameters = "Linha=Região_(capitais)&Coluna=--Não-Ativa--&Incremento=Prevalência_diabete_melito&Arquivos=vigtel10.dbf&SRegião(capitais)=TODAS_AS_CATEGORIAS__&SCapital=TODAS_AS_CATEGORIAS__&SSexo=TODAS_AS_CATEGORIAS__&SFaixa_etária=TODAS_AS_CATEGORIAS__&SEscolaridade=TODAS_AS_CATEGORIAS__&mostre=Mostra";
         final String encodedParams = URLEncoder.encode(urlParameters, "ISO8859-1");
         System.out.println(executePost(targetURL, encodedParams));
         */

         final String targetURL = "http://tabnet.datasus.gov.br/cgi/tabcgi.exe?idb2011/c06.def";
         //final String targetURL = "http://tabnet.datasus.gov.br/cgi/tabnet.exe?idb2011/a16.def";
         final String urlParameters = //"Incremento=Proporção_de_óbitos_(%)&Arquivos=obim09.dbf&SRegião_Metropolitana=TODAS_AS_CATEGORIAS__&SUnidade_da_Federação=TODAS_AS_CATEGORIAS__&Linha=Região_Metropolitana&SRegião=TODAS_AS_CATEGORIAS__&Coluna=--Não-Ativa--&SCapital=TODAS_AS_CATEGORIAS__";
                                      //"Incremento=Proporção_de_óbitos_(%)&Arquivos=obim10.dbf&SRegião_Metropolitana=TODAS_AS_CATEGORIAS__&SUnidade_da_Federação=TODAS_AS_CATEGORIAS__&Linha=Unidade_da_Federação&SRegião=TODAS_AS_CATEGORIAS__&Coluna=--Não-Ativa--&SCapital=TODAS_AS_CATEGORIAS__";
                                       //"Incremento=Proporção_de_óbitos_(%)&Arquivos=obim08.dbf&SRegião_Metropolitana=TODAS_AS_CATEGORIAS__&SUnidade_da_Federação=TODAS_AS_CATEGORIAS__&Linha=Capital&SRegião=TODAS_AS_CATEGORIAS__&Coluna=--Não-Ativa--&SCapital=TODAS_AS_CATEGORIAS__";
                                       "Incremento=Núm._óbitos_doenças_diarreicas&Arquivos=obim10.dbf&SRegião_Metropolitana=TODAS_AS_CATEGORIAS__&SUnidade_da_Federação=TODAS_AS_CATEGORIAS__&Linha=Região_Metropolitana&SRegião=TODAS_AS_CATEGORIAS__&Coluna=--Não-Ativa--&SCapital=TODAS_AS_CATEGORIAS__";
         final String encodedParams = URLEncoder.encode(urlParameters, "ISO8859-1");

         //System.out.println(executePost(targetURL, encodedParams));
         System.out.println("\n\n\n");
         final String[] content = URLS.loadPagePost(new URL(targetURL), urlParameters);
         System.out.println(content[1]);
     }

}
