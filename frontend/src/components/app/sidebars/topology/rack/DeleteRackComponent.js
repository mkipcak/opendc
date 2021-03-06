import React from 'react'

const DeleteRackComponent = ({ onClick }) => (
    <div className="btn btn-outline-danger btn-block" onClick={onClick}>
        <span className="fa fa-trash mr-2" />
        Delete this rack
    </div>
)

export default DeleteRackComponent
