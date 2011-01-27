/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.edal.cdm.kdtree;

/**
 *
 * @author andy
 */
public class ResultException extends Exception {
    int file_index;

    public ResultException(int file_index) {
        this.file_index = file_index;
    }
}
